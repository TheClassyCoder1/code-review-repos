package webhooks

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"artifactd/internal/models"
)

type Dispatcher struct {
	client *http.Client
}

func New(timeout time.Duration) *Dispatcher {
	return &Dispatcher{client: &http.Client{
		Timeout: timeout,
		CheckRedirect: func(*http.Request, []*http.Request) error {
			return http.ErrUseLastResponse
		},
	}}
}

func sign(secret string, body []byte) string {
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write(body)
	return "sha256=" + hex.EncodeToString(mac.Sum(nil))
}

type Event struct {
	Type       string    `json:"type"`
	Repository string    `json:"repository"`
	Tag        string    `json:"tag"`
	Digest     string    `json:"digest"`
	OccurredAt time.Time `json:"occurred_at"`
}

func (d *Dispatcher) Deliver(ctx context.Context, hook models.Webhook, ev Event) error {
	body, err := json.Marshal(ev)
	if err != nil {
		return err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, hook.URL, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Artifactd-Event", ev.Type)
	req.Header.Set("X-Artifactd-Signature", sign(hook.Secret, body))
	resp, err := d.client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		return fmt.Errorf("webhook %d returned %s", hook.ID, resp.Status)
	}
	return nil
}
