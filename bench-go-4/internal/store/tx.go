package store

import (
	"context"

	"github.com/jackc/pgx/v5"
)

// InTx runs fn inside a transaction, committing when fn returns nil.
func (s *Store) InTx(ctx context.Context, fn func(tx pgx.Tx) error) error {
	return pgx.BeginFunc(ctx, s.pool, fn)
}

// WithCampaignLock runs fn in a transaction holding a row lock on the
// campaign's budget, so concurrent budget changes for it are serialized.
func (s *Store) WithCampaignLock(ctx context.Context, campaignID int64, fn func(tx pgx.Tx) error) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		var id int64
		err := tx.QueryRow(ctx,
			`SELECT campaign_id FROM budgets WHERE campaign_id = $1 FOR UPDATE`, campaignID).Scan(&id)
		if err == pgx.ErrNoRows {
			return ErrNotFound
		}
		if err != nil {
			return err
		}
		return fn(tx)
	})
}
