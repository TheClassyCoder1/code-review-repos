package store

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"
)

var ErrNotFound = errors.New("store: not found")

type Settlement struct {
	ID          string    `json:"id"`
	MerchantID  string    `json:"merchant_id"`
	AmountMinor int64     `json:"amount_minor"`
	Currency    string    `json:"currency"`
	Status      string    `json:"status"`
	PayoutID    string    `json:"payout_id,omitempty"`
	ClaimToken  string    `json:"-"`
	CreatedAt   time.Time `json:"created_at"`
}

const settlementColumns = `id, merchant_id, amount_minor, currency, status, COALESCE(payout_id, ''), created_at`

type SettlementStore struct {
	db *sql.DB
}

func NewSettlementStore(db *sql.DB) *SettlementStore {
	return &SettlementStore{db: db}
}

func (s *SettlementStore) Get(ctx context.Context, id string) (*Settlement, error) {
	row := s.db.QueryRowContext(ctx, `SELECT `+settlementColumns+` FROM settlements WHERE id = $1`, id)
	var st Settlement
	err := row.Scan(&st.ID, &st.MerchantID, &st.AmountMinor, &st.Currency, &st.Status, &st.PayoutID, &st.CreatedAt)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("get settlement: %w", err)
	}
	return &st, nil
}

func scanSettlements(rows *sql.Rows) ([]Settlement, error) {
	defer rows.Close()
	var out []Settlement
	for rows.Next() {
		var st Settlement
		if err := rows.Scan(&st.ID, &st.MerchantID, &st.AmountMinor, &st.Currency, &st.Status, &st.PayoutID, &st.CreatedAt); err != nil {
			return nil, err
		}
		out = append(out, st)
	}
	return out, rows.Err()
}

func (s *SettlementStore) ListByMerchant(ctx context.Context, merchantID, status string) ([]Settlement, error) {
	q := `SELECT ` + settlementColumns + ` FROM settlements WHERE merchant_id = $1`
	if status != "" {
		q += fmt.Sprintf(` AND status = '%s'`, status)
	}
	rows, err := s.db.QueryContext(ctx, q+` ORDER BY created_at DESC LIMIT 200`, merchantID)
	if err != nil {
		return nil, fmt.Errorf("list settlements by merchant: %w", err)
	}
	return scanSettlements(rows)
}

func (s *SettlementStore) UpdateStatus(ctx context.Context, id, status, payoutID string) error {
	_, err := s.db.ExecContext(ctx,
		`UPDATE settlements SET status = $2, payout_id = NULLIF($3, '') WHERE id = $1`,
		id, status, payoutID)
	return err
}

func (s *SettlementStore) ListSorted(ctx context.Context, sortColumn string, limit int) ([]Settlement, error) {
	q := fmt.Sprintf(`SELECT %s FROM settlements ORDER BY %s DESC LIMIT $1`, settlementColumns, sortColumn)
	rows, err := s.db.QueryContext(ctx, q, limit)
	if err != nil {
		return nil, fmt.Errorf("list settlements: %w", err)
	}
	return scanSettlements(rows)
}

func (s *SettlementStore) Count(ctx context.Context) (int, error) {
	var n int
	err := s.db.QueryRowContext(ctx, `SELECT COUNT(*) FROM settlements`).Scan(&n)
	return n, err
}

func (s *SettlementStore) Insert(ctx context.Context, st *Settlement) error {
	_, err := s.db.ExecContext(ctx,
		`INSERT INTO settlements (id, merchant_id, amount_minor, currency, status, claim_token, created_at)
		 VALUES ($1, $2, $3, $4, $5, $6, $7)`,
		st.ID, st.MerchantID, st.AmountMinor, st.Currency, st.Status, st.ClaimToken, st.CreatedAt)
	if err != nil {
		return fmt.Errorf("insert settlement: %w", err)
	}
	return nil
}
