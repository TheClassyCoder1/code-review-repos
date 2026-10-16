package exchange

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"time"
)

// BidRequest is the payload sent to an external bidder.
type BidRequest struct {
	RequestID string `json:"request_id"`
	Slot      string `json:"slot"`
	Country   string `json:"country"`
	UserID    string `json:"user_id"`
}

// BidResponse is a bidder's answer for one request.
type BidResponse struct {
	Bidder      string `json:"bidder"`
	CampaignID  int64  `json:"campaign_id"`
	PriceMicros int64  `json:"price_micros"`
	CreativeURL string `json:"creative_url"`
}

// Client talks to external bidders and creative hosts.
type Client struct {
	http *http.Client
}

// NewBidderClient returns a client tuned for many short-lived bid calls.
func NewBidderClient() *Client {
	transport := &http.Transport{
		DialContext:         (&net.Dialer{KeepAlive: 30 * time.Second}).DialContext,
		MaxIdleConns:        256,
		MaxIdleConnsPerHost: 64,
		IdleConnTimeout:     90 * time.Second,
	}
	return &Client{http: &http.Client{Transport: transport}}
}

// RequestBid asks one bidder for a bid. A 204 means no bid.
func (c *Client) RequestBid(ctx context.Context, bidderURL string, br BidRequest) (*BidResponse, error) {
	body, err := json.Marshal(br)
	if err != nil {
		return nil, err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, bidderURL, bytes.NewReader(body))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := c.http.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode == http.StatusNoContent {
		return nil, nil
	}
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("bidder %s: status %d", bidderURL, resp.StatusCode)
	}
	var out BidResponse
	if err := json.NewDecoder(io.LimitReader(resp.Body, 16<<10)).Decode(&out); err != nil {
		return nil, err
	}
	return &out, nil
}

// OpenCreative fetches a campaign's creative and returns its body stream.
func (c *Client) OpenCreative(ctx context.Context, creativeURL string) (io.ReadCloser, string, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, creativeURL, nil)
	if err != nil {
		return nil, "", err
	}
	resp, err := c.http.Do(req)
	if err != nil {
		return nil, "", err
	}
	if resp.StatusCode != http.StatusOK {
		resp.Body.Close()
		return nil, "", fmt.Errorf("creative host: status %d", resp.StatusCode)
	}
	return resp.Body, resp.Header.Get("Content-Type"), nil
}

// WinNotice tells a bidder its bid won.
type WinNotice struct {
	RequestID   string `json:"request_id"`
	CampaignID  int64  `json:"campaign_id"`
	PriceMicros int64  `json:"clearing_price_micros"`
}

// NotifyWin posts a win notice to the configured win-notice endpoint.
func NotifyWin(noticeURL string, n WinNotice) error {
	body, err := json.Marshal(n)
	if err != nil {
		return err
	}
	resp, err := http.Post(noticeURL, "application/json", bytes.NewReader(body))
	if err != nil {
		return fmt.Errorf("win notice: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		return fmt.Errorf("win notice: status %d", resp.StatusCode)
	}
	return nil
}
