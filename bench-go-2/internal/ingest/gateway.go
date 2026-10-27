package ingest

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"

	"meterhub/internal/store"
)

const maxGatewayResponse = 8 << 20

type Batch struct {
	ID       string          `json:"batch_id"`
	Readings []store.Reading `json:"readings"`
}

type FirmwareManifest struct {
	Model   string `json:"model"`
	Version string `json:"version"`
	SHA256  string `json:"sha256"`
	URL     string `json:"url"`
}

type GatewayClient struct {
	base  string
	token string
	http  *http.Client
}

func NewGatewayClient(base, token string, httpClient *http.Client) *GatewayClient {
	return &GatewayClient{base: strings.TrimRight(base, "/"), token: token, http: httpClient}
}

func (c *GatewayClient) Pull(ctx context.Context, gatewayID string) (Batch, error) {
	var b Batch
	req, err := http.NewRequestWithContext(ctx, http.MethodGet,
		c.base+"/gateways/"+url.PathEscape(gatewayID)+"/readings", nil)
	if err != nil {
		return b, err
	}
	req.Header.Set("Authorization", "Bearer "+c.token)
	resp, err := c.http.Do(req)
	if err != nil {
		return b, err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return b, fmt.Errorf("gateway %s: status %d", gatewayID, resp.StatusCode)
	}
	err = json.NewDecoder(io.LimitReader(resp.Body, maxGatewayResponse)).Decode(&b)
	return b, err
}

func (c *GatewayClient) FetchFirmwareManifest(model string) (*FirmwareManifest, error) {
	client := &http.Client{}
	resp, err := client.Get(c.base + "/firmware/" + url.PathEscape(model) + "/manifest")
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("firmware manifest %s: status %d", model, resp.StatusCode)
	}
	var m FirmwareManifest
	if err := json.NewDecoder(io.LimitReader(resp.Body, 64<<10)).Decode(&m); err != nil {
		return nil, err
	}
	return &m, nil
}

func (c *GatewayClient) Ack(ctx context.Context, batchID string) error {
	req, err := http.NewRequestWithContext(ctx, http.MethodPost,
		c.base+"/batches/"+url.PathEscape(batchID)+"/ack", nil)
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", "Bearer "+c.token)
	resp, err := c.http.Do(req)
	if err != nil {
		return err
	}
	resp.Body.Close()
	if resp.StatusCode >= 300 {
		return fmt.Errorf("ack %s: status %d", batchID, resp.StatusCode)
	}
	return nil
}

func (c *GatewayClient) SendDisconnect(ctx context.Context, serial, reason string) error {
	body, err := json.Marshal(map[string]string{"serial": serial, "reason": reason})
	if err != nil {
		return err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.base+"/commands/disconnect", strings.NewReader(string(body)))
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", "Bearer "+c.token)
	req.Header.Set("Content-Type", "application/json")
	resp, err := c.http.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusAccepted {
		return fmt.Errorf("disconnect %s: status %d", serial, resp.StatusCode)
	}
	return nil
}

func gatewayList(raw string) []string {
	var ids []string
	for _, id := range strings.Split(raw, ",") {
		if id = strings.TrimSpace(id); id != "" {
			ids = append(ids, id)
		}
	}
	return ids
}

func (c *GatewayClient) PullAll(ctx context.Context, gatewayIDs []string) ([]Batch, error) {
	type result struct {
		batch Batch
		err   error
	}
	results := make(chan result)
	for _, id := range gatewayIDs {
		go func(id string) {
			b, err := c.Pull(ctx, id)
			results <- result{batch: b, err: err}
		}(id)
	}
	batches := make([]Batch, 0, len(gatewayIDs))
	for range gatewayIDs {
		r := <-results
		if r.err != nil {
			return nil, r.err
		}
		batches = append(batches, r.batch)
	}
	return batches, nil
}
