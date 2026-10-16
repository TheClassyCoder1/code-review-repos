package store

import (
	"context"
	"encoding/csv"
	"fmt"
	"io"
	"strconv"
	"time"
)

// Impression is one served ad impression.
type Impression struct {
	ID          string    `json:"id"`
	CampaignID  int64     `json:"campaign_id"`
	UserID      string    `json:"user_id"`
	PriceMicros int64     `json:"price_micros"`
	ServedAt    time.Time `json:"served_at"`
}

// SpendRow is one day of spend for a campaign.
type SpendRow struct {
	Day         time.Time `json:"day"`
	Impressions int64     `json:"impressions"`
	SpendMicros int64     `json:"spend_micros"`
}

// RecordImpression inserts an impression; it reports false when the
// impression id was already recorded.
func (s *Store) RecordImpression(ctx context.Context, imp Impression) (bool, error) {
	tag, err := s.pool.Exec(ctx,
		`INSERT INTO impressions (id, campaign_id, user_id, price_micros, served_at)
		 VALUES ($1, $2, $3, $4, $5) ON CONFLICT (id) DO NOTHING`,
		imp.ID, imp.CampaignID, imp.UserID, imp.PriceMicros, imp.ServedAt)
	if err != nil {
		return false, fmt.Errorf("record impression: %w", err)
	}
	return tag.RowsAffected() == 1, nil
}

// CountUserImpressions returns how many impressions of a campaign a user has
// seen since the given time.
func (s *Store) CountUserImpressions(ctx context.Context, campaignID int64, userID string, since time.Time) (int, error) {
	var n int
	err := s.pool.QueryRow(ctx,
		`SELECT count(*) FROM impressions WHERE campaign_id = $1 AND user_id = $2 AND served_at >= $3`,
		campaignID, userID, since).Scan(&n)
	return n, err
}

// ExportImpressions writes a campaign's impressions for a day as CSV.
func (s *Store) ExportImpressions(ctx context.Context, campaignID int64, day time.Time, w io.Writer) error {
	rows, err := s.pool.Query(ctx,
		`SELECT id, user_id, price_micros, served_at FROM impressions
		 WHERE campaign_id = $1 AND served_at >= $2 AND served_at < $3 ORDER BY served_at`,
		campaignID, day, day.AddDate(0, 0, 1))
	if err != nil {
		return err
	}
	cw := csv.NewWriter(w)
	if err := cw.Write([]string{"id", "user_id", "price_micros", "served_at"}); err != nil {
		return err
	}
	for rows.Next() {
		var imp Impression
		if err := rows.Scan(&imp.ID, &imp.UserID, &imp.PriceMicros, &imp.ServedAt); err != nil {
			return err
		}
		rec := []string{imp.ID, imp.UserID, strconv.FormatInt(imp.PriceMicros, 10), imp.ServedAt.Format(time.RFC3339)}
		if err := cw.Write(rec); err != nil {
			return err
		}
	}
	cw.Flush()
	if err := rows.Err(); err != nil {
		return err
	}
	return cw.Error()
}

// DailySpend returns per-day spend for a campaign over the last n days.
func (s *Store) DailySpend(ctx context.Context, campaignID int64, days int) ([]SpendRow, error) {
	rows, err := s.pool.Query(ctx,
		`SELECT date_trunc('day', served_at), count(*), coalesce(sum(price_micros), 0)
		 FROM impressions WHERE campaign_id = $1 AND served_at >= now() - make_interval(days => $2)
		 GROUP BY 1 ORDER BY 1`, campaignID, days)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []SpendRow
	for rows.Next() {
		var r SpendRow
		if err := rows.Scan(&r.Day, &r.Impressions, &r.SpendMicros); err != nil {
			return nil, err
		}
		out = append(out, r)
	}
	return out, rows.Err()
}

// ListImpressions returns one page of a campaign's impressions, newest first.
func (s *Store) ListImpressions(ctx context.Context, campaignID int64, page, size int) ([]Impression, error) {
	offset := (page - 1) * size
	rows, err := s.pool.Query(ctx,
		`SELECT id, campaign_id, user_id, price_micros, served_at FROM impressions
		 WHERE campaign_id = $1 ORDER BY served_at DESC LIMIT $2 OFFSET $3`,
		campaignID, size, offset)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := make([]Impression, 0, size)
	for rows.Next() {
		var imp Impression
		if err := rows.Scan(&imp.ID, &imp.CampaignID, &imp.UserID, &imp.PriceMicros, &imp.ServedAt); err != nil {
			return nil, err
		}
		out = append(out, imp)
	}
	return out, rows.Err()
}

// Conversion is a post-click or post-view conversion reported by an
// advertiser's tracking pixel.
type Conversion struct {
	ImpressionID string    `json:"impression_id"`
	ValueMicros  int64     `json:"value_micros"`
	OccurredAt   time.Time `json:"occurred_at"`
}

// RecordConversion stores a conversion, ignoring repeats for the same impression.
func (s *Store) RecordConversion(ctx context.Context, c Conversion) error {
	_, err := s.pool.Exec(ctx,
		`INSERT INTO conversions (impression_id, value_micros, occurred_at)
		 VALUES ($1, $2, $3) ON CONFLICT (impression_id) DO NOTHING`,
		c.ImpressionID, c.ValueMicros, c.OccurredAt)
	return err
}
