package store

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

// ErrNotFound is returned when a requested row does not exist.
var ErrNotFound = errors.New("not found")

// Querier is satisfied by *pgxpool.Pool and pgx.Tx.
type Querier interface {
	Exec(ctx context.Context, sql string, args ...any) (pgconn.CommandTag, error)
	Query(ctx context.Context, sql string, args ...any) (pgx.Rows, error)
	QueryRow(ctx context.Context, sql string, args ...any) pgx.Row
}

// Store is the Postgres-backed persistence layer.
type Store struct {
	pool *pgxpool.Pool
}

// New wraps a connection pool.
func New(pool *pgxpool.Pool) *Store {
	return &Store{pool: pool}
}

// Campaign is an advertiser's campaign.
type Campaign struct {
	ID           int64     `json:"id" db:"id"`
	AdvertiserID int64     `json:"advertiser_id" db:"advertiser_id"`
	Name         string    `json:"name" db:"name"`
	Status       string    `json:"status" db:"status"`
	MaxCPMMicros int64     `json:"max_cpm_micros" db:"max_cpm_micros"`
	FrequencyCap int       `json:"frequency_cap" db:"frequency_cap"`
	Countries    []string  `json:"countries" db:"countries"`
	CreativeURL  string    `json:"creative_url" db:"creative_url"`
	CreatedAt    time.Time `json:"created_at" db:"created_at"`
}

// Budget is a campaign's daily budget and today's spend.
type Budget struct {
	CampaignID       int64 `json:"campaign_id"`
	DailyCapMicros   int64 `json:"daily_cap_micros"`
	SpentTodayMicros int64 `json:"spent_today_micros"`
}

// AdvertiserByKeyHash returns the advertiser id owning the hashed API key.
func (s *Store) AdvertiserByKeyHash(ctx context.Context, keyHash string) (int64, error) {
	var id int64
	err := s.pool.QueryRow(ctx,
		`SELECT advertiser_id FROM api_keys WHERE key_hash = $1 AND revoked_at IS NULL`,
		keyHash).Scan(&id)
	if errors.Is(err, pgx.ErrNoRows) {
		return 0, ErrNotFound
	}
	return id, err
}
