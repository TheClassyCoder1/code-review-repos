package store

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
)

func (d *DB) GetTariff(ctx context.Context, id int64) (*Tariff, error) {
	var t Tariff
	var tiers []byte
	err := d.QueryRowContext(ctx,
		`SELECT id, name, standing_cents_per_day, tiers FROM tariffs WHERE id = $1`, id).
		Scan(&t.ID, &t.Name, &t.StandingCentsPerDay, &tiers)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, err
	}
	if err := json.Unmarshal(tiers, &t.Tiers); err != nil {
		return nil, err
	}
	return &t, nil
}

func (d *DB) UpsertTariffs(ctx context.Context, tariffs []Tariff) error {
	return d.WithTx(ctx, func(tx *sql.Tx) error {
		for _, t := range tariffs {
			tiers, err := json.Marshal(t.Tiers)
			if err != nil {
				return err
			}
			_, err = tx.ExecContext(ctx,
				`INSERT INTO tariffs (name, standing_cents_per_day, tiers) VALUES ($1, $2, $3)
				 ON CONFLICT (name) DO UPDATE SET standing_cents_per_day = EXCLUDED.standing_cents_per_day, tiers = EXCLUDED.tiers`,
				t.Name, t.StandingCentsPerDay, tiers)
			if err != nil {
				return err
			}
		}
		return nil
	})
}
