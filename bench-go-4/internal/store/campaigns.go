package store

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
)

const campaignColumns = `id, advertiser_id, name, status, max_cpm_micros, frequency_cap, countries, creative_url, created_at`

// GetCampaign returns the campaign with the given id.
func (s *Store) GetCampaign(ctx context.Context, id int64) (*Campaign, error) {
	rows, err := s.pool.Query(ctx, `SELECT `+campaignColumns+` FROM campaigns WHERE id = $1`, id)
	if err != nil {
		return nil, err
	}
	c, err := pgx.CollectExactlyOneRow(rows, pgx.RowToAddrOfStructByName[Campaign])
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("get campaign %d: %w", id, err)
	}
	return c, nil
}

// SetStatus changes a campaign's delivery status.
func (s *Store) SetStatus(ctx context.Context, id int64, status string) error {
	tag, err := s.pool.Exec(ctx, `UPDATE campaigns SET status = $2 WHERE id = $1`, id, status)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return ErrNotFound
	}
	return nil
}

// ListCampaigns returns an advertiser's campaigns, newest first.
func (s *Store) ListCampaigns(ctx context.Context, advertiserID int64) ([]Campaign, error) {
	rows, err := s.pool.Query(ctx,
		`SELECT `+campaignColumns+` FROM campaigns WHERE advertiser_id = $1 ORDER BY created_at DESC`,
		advertiserID)
	if err != nil {
		return nil, err
	}
	return pgx.CollectRows(rows, pgx.RowToStructByName[Campaign])
}

// NewCampaign holds the fields needed to create a campaign.
type NewCampaign struct {
	AdvertiserID   int64
	Name           string
	MaxCPMMicros   int64
	FrequencyCap   int
	Countries      []string
	CreativeURL    string
	DailyCapMicros int64
}

// CreateCampaignWithBudget creates a campaign and its daily budget row.
func (s *Store) CreateCampaignWithBudget(ctx context.Context, nc NewCampaign) (int64, error) {
	var id int64
	err := s.pool.QueryRow(ctx,
		`INSERT INTO campaigns (advertiser_id, name, status, max_cpm_micros, frequency_cap, countries, creative_url)
		 VALUES ($1, $2, 'active', $3, $4, $5, $6) RETURNING id`,
		nc.AdvertiserID, nc.Name, nc.MaxCPMMicros, nc.FrequencyCap, nc.Countries, nc.CreativeURL).Scan(&id)
	if err != nil {
		return 0, fmt.Errorf("insert campaign: %w", err)
	}
	if _, err := s.pool.Exec(ctx,
		`INSERT INTO budgets (campaign_id, daily_cap_micros, spent_today_micros) VALUES ($1, $2, 0)`,
		id, nc.DailyCapMicros); err != nil {
		return 0, fmt.Errorf("insert budget: %w", err)
	}
	return id, nil
}

// ActiveCampaignCount returns how many of an advertiser's campaigns are active.
func (s *Store) ActiveCampaignCount(ctx context.Context, advertiserID int64) (int, error) {
	var n int
	err := s.pool.QueryRow(ctx,
		`SELECT count(*) FROM campaigns WHERE advertiser_id = $1 AND status = 'active'`, advertiserID).Scan(&n)
	return n, err
}

// ArchiveCampaign marks a campaign archived and drops its queued bids.
func ArchiveCampaign(ctx context.Context, q Querier, id int64) error {
	if _, err := q.Exec(ctx, `UPDATE campaigns SET status = 'archived' WHERE id = $1`, id); err != nil {
		return fmt.Errorf("archive campaign: %w", err)
	}
	if _, err := q.Exec(ctx, `DELETE FROM pending_bids WHERE campaign_id = $1`, id); err != nil {
		return fmt.Errorf("drop pending bids: %w", err)
	}
	if _, err := q.Exec(ctx, `UPDATE budgets SET daily_cap_micros = 0 WHERE campaign_id = $1`, id); err != nil {
		return fmt.Errorf("zero budget: %w", err)
	}
	return nil
}
