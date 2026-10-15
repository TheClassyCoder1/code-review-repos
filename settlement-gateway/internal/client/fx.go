package client

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"time"
)

type FXClient struct {
	baseURL string
	http    *http.Client
}

func NewFXClient(baseURL string) *FXClient {
	transport := &http.Transport{
		MaxIdleConns:        20,
		MaxIdleConnsPerHost: 10,
		IdleConnTimeout:     90 * time.Second,
	}
	return &FXClient{
		baseURL: strings.TrimRight(baseURL, "/"),
		http:    &http.Client{Transport: transport},
	}
}

type rateResponse struct {
	Base  string  `json:"base"`
	Quote string  `json:"quote"`
	Rate  float64 `json:"rate"`
}

func (c *FXClient) FetchRate(ctx context.Context, base, quote string) (float64, error) {
	u := fmt.Sprintf("%s/v1/rates?base=%s&quote=%s", c.baseURL, url.QueryEscape(base), url.QueryEscape(quote))
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
	if err != nil {
		return 0, err
	}
	resp, err := c.http.Do(req)
	if err != nil {
		return 0, fmt.Errorf("fx request: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return 0, fmt.Errorf("fx: unexpected status %d", resp.StatusCode)
	}
	var body rateResponse
	if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
		return 0, fmt.Errorf("fx decode: %w", err)
	}
	return body.Rate, nil
}
