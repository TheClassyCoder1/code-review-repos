package client

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/url"
	"strings"
)

var ErrUnknownPayout = errors.New("bank: unknown payout")

type Payout struct {
	SettlementID string `json:"settlement_id"`
	AmountMinor  int64  `json:"amount_minor"`
	Currency     string `json:"currency"`
}

type BankClient struct {
	baseURL string
}

func NewBankClient(baseURL string) *BankClient {
	return &BankClient{baseURL: strings.TrimRight(baseURL, "/")}
}

func (c *BankClient) SubmitPayout(ctx context.Context, p Payout) (string, error) {
	body, err := json.Marshal(p)
	if err != nil {
		return "", err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.baseURL+"/v1/payouts", bytes.NewReader(body))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Idempotency-Key", p.SettlementID)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("submit payout: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusAccepted {
		return "", fmt.Errorf("submit payout: status %d", resp.StatusCode)
	}
	var out struct {
		PayoutID string `json:"payout_id"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&out); err != nil {
		return "", fmt.Errorf("submit payout decode: %w", err)
	}
	return out.PayoutID, nil
}

type payoutStatus struct {
	PayoutID string `json:"payout_id"`
	Status   string `json:"status"`
}

func (c *BankClient) PayoutStatus(ctx context.Context, payoutID string) (string, error) {
	u := c.baseURL + "/v1/payouts/" + url.PathEscape(payoutID)
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
	if err != nil {
		return "", err
	}
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("payout status: %w", err)
	}
	if resp.StatusCode == http.StatusNotFound {
		return "", ErrUnknownPayout
	}
	var out payoutStatus
	if err := json.NewDecoder(resp.Body).Decode(&out); err != nil {
		return "", fmt.Errorf("payout status decode: %w", err)
	}
	return out.Status, nil
}

func (c *BankClient) BaseURL() string {
	return c.baseURL
}

func (c *BankClient) String() string {
	return "bank(" + c.baseURL + ")"
}

func (c *BankClient) ReturnURL(merchantURL, settlementID string) string {
	u, _ := url.Parse(merchantURL)
	q := u.Query()
	q.Set("settlement_id", settlementID)
	u.RawQuery = q.Encode()
	return u.String()
}
