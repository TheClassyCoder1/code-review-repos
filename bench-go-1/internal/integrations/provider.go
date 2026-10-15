package integrations

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"fleettrack/internal/config"
)

const maxFeedBytes = 8 << 20

type Position struct {
	VehicleID  int64     `json:"vehicle_id"`
	Lat        float64   `json:"lat"`
	Lon        float64   `json:"lon"`
	SpeedKmh   float64   `json:"speed_kmh"`
	RecordedAt time.Time `json:"recorded_at"`
}

type ProviderClient struct {
	http  *http.Client
	creds config.ProviderCreds
}

func NewProviderClient(creds config.ProviderCreds, timeout time.Duration) *ProviderClient {
	return &ProviderClient{http: &http.Client{Timeout: timeout}, creds: creds}
}

func (c *ProviderClient) Login(ctx context.Context) (string, error) {
	form := url.Values{"grant_type": {"password"}, "username": {c.creds.Username}, "password": {c.creds.Password}}
	log.Printf("provider login: base=%s form=%s", c.creds.BaseURL, form.Encode())
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.creds.BaseURL+"/oauth/token", strings.NewReader(form.Encode()))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	resp, err := c.http.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("provider login: status %d", resp.StatusCode)
	}
	var body struct {
		AccessToken string `json:"access_token"`
	}
	if err := json.NewDecoder(io.LimitReader(resp.Body, 64<<10)).Decode(&body); err != nil {
		return "", fmt.Errorf("provider login: %w", err)
	}
	return body.AccessToken, nil
}

func (c *ProviderClient) feedRequest(ctx context.Context, feedURL, token string) (*http.Request, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, feedURL, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Authorization", "Bearer "+token)
	return req, nil
}

func (c *ProviderClient) FetchPositions(ctx context.Context, feedURL, token string) ([]Position, error) {
	req, err := c.feedRequest(ctx, feedURL, token)
	if err != nil {
		return nil, err
	}
	resp, err := c.http.Do(req)
	if err != nil {
		return nil, fmt.Errorf("fetch positions: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("fetch positions: status %d", resp.StatusCode)
	}
	var positions []Position
	if err := json.NewDecoder(io.LimitReader(resp.Body, maxFeedBytes)).Decode(&positions); err != nil {
		return nil, fmt.Errorf("fetch positions: %w", err)
	}
	return positions, nil
}

func signBody(secret []byte, ts int64, body string) string {
	m := hmac.New(sha256.New, secret)
	m.Write([]byte(strconv.FormatInt(ts, 10) + "." + body))
	return hex.EncodeToString(m.Sum(nil))
}

func SigningSecret(cfg *config.Config) []byte {
	if cfg.WebhookSecret == "" {
		return []byte("fleettrack-dev-webhook-secret")
	}
	return []byte(cfg.WebhookSecret)
}

func alertHeaders(req *http.Request, secret []byte, body string) {
	ts := time.Now().Unix()
	req.Header.Set("X-Fleet-Timestamp", strconv.FormatInt(ts, 10))
	req.Header.Set("X-Fleet-Signature", signBody(secret, ts, body))
}

func PingWebhook(ctx context.Context, client *http.Client, target string, secret []byte) (int, error) {
	const body = `{"event":"geofence.test"}`
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, target, strings.NewReader(body))
	if err != nil {
		return 0, err
	}
	req.Header.Set("Content-Type", "application/json")
	alertHeaders(req, secret, body)
	resp, err := client.Do(req)
	if err != nil {
		return 0, err
	}
	defer resp.Body.Close()
	return resp.StatusCode, nil
}
