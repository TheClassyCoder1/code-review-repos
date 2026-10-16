package store

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
)

// ErrBudgetExhausted is returned when a spend would exceed the daily cap.
var ErrBudgetExhausted = errors.New("daily budget exhausted")

// FindBudget returns the campaign's budget, or nil when it has none.
func (s *Store) FindBudget(ctx context.Context, campaignID int64) (*Budget, error) {
	b := &Budget{CampaignID: campaignID}
	err := s.pool.QueryRow(ctx,
		`SELECT daily_cap_micros, spent_today_micros FROM budgets WHERE campaign_id = $1`,
		campaignID).Scan(&b.DailyCapMicros, &b.SpentTodayMicros)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return b, nil
}

// ReserveSpend records priceMicros of spend against today's budget if it
// still fits under the daily cap.
func (s *Store) ReserveSpend(ctx context.Context, campaignID, priceMicros int64) error {
	var capMicros, spent int64
	err := s.pool.QueryRow(ctx,
		`SELECT daily_cap_micros, spent_today_micros FROM budgets WHERE campaign_id = $1`,
		campaignID).Scan(&capMicros, &spent)
	if err != nil {
		return fmt.Errorf("read budget: %w", err)
	}
	if spent+priceMicros > capMicros {
		return ErrBudgetExhausted
	}
	_, err = s.pool.Exec(ctx,
		`UPDATE budgets SET spent_today_micros = spent_today_micros + $2 WHERE campaign_id = $1`,
		campaignID, priceMicros)
	return err
}

// PacedToday returns the pacer's estimated spend today for a campaign.
func (s *Store) PacedToday(ctx context.Context, campaignID int64) (int64, error) {
	var spent int64
	err := s.pool.QueryRow(ctx,
		`SELECT paced_today_micros FROM budgets WHERE campaign_id = $1`, campaignID).Scan(&spent)
	return spent, err
}

// SetPacedToday overwrites the pacer's estimated spend today for a campaign.
func (s *Store) SetPacedToday(ctx context.Context, campaignID, spent int64) error {
	_, err := s.pool.Exec(ctx,
		`UPDATE budgets SET paced_today_micros = $2 WHERE campaign_id = $1`, campaignID, spent)
	return err
}

// RaiseDailyCap increases a campaign's daily cap by deltaMicros within tx and
// returns the new cap.
func RaiseDailyCap(ctx context.Context, tx pgx.Tx, campaignID, deltaMicros int64) (int64, error) {
	var current int64
	if err := tx.QueryRow(ctx,
		`SELECT daily_cap_micros FROM budgets WHERE campaign_id = $1`, campaignID).Scan(&current); err != nil {
		return 0, fmt.Errorf("read cap: %w", err)
	}
	next := current + deltaMicros
	if _, err := tx.Exec(ctx,
		`UPDATE budgets SET daily_cap_micros = $2 WHERE campaign_id = $1`, campaignID, next); err != nil {
		return 0, fmt.Errorf("write cap: %w", err)
	}
	return next, nil
}
