package store

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"

	_ "github.com/lib/pq"
)

var (
	ErrNotFound              = errors.New("not found")
	ErrDuplicateSerial       = errors.New("meter serial already registered")
	ErrTariffAlreadyAssigned = errors.New("tariff already assigned for that date")
)

type Querier interface {
	ExecContext(ctx context.Context, query string, args ...any) (sql.Result, error)
	QueryContext(ctx context.Context, query string, args ...any) (*sql.Rows, error)
	QueryRowContext(ctx context.Context, query string, args ...any) *sql.Row
}

type DB struct {
	*sql.DB
}

type Account struct {
	ID           int64  `json:"id"`
	Name         string `json:"name"`
	Email        string `json:"email"`
	BalanceCents int64  `json:"balance_cents"`
}

type Meter struct {
	ID            int64      `json:"id"`
	AccountID     int64      `json:"account_id"`
	Serial        string     `json:"serial"`
	Name          string     `json:"name"`
	Status        string     `json:"status"`
	ProvisionHash string     `json:"-"`
	LastReadingAt *time.Time `json:"last_reading_at,omitempty"`
	BilledUntil   *time.Time `json:"billed_until,omitempty"`
	CreatedAt     time.Time  `json:"created_at"`
}

type Reading struct {
	MeterID   int64     `json:"meter_id"`
	ReadAt    time.Time `json:"read_at"`
	WattHours int64     `json:"watt_hours"`
}

type Tier struct {
	UpToWh           int64 `json:"up_to_wh"`
	MilliCentsPerKWh int64 `json:"milli_cents_per_kwh"`
}

type Tariff struct {
	ID                  int64  `json:"id"`
	Name                string `json:"name"`
	StandingCentsPerDay int64  `json:"standing_cents_per_day"`
	Tiers               []Tier `json:"tiers"`
}

type Bill struct {
	ID          int64     `json:"id"`
	MeterID     int64     `json:"meter_id"`
	AccountID   int64     `json:"account_id"`
	PeriodStart time.Time `json:"period_start"`
	PeriodEnd   time.Time `json:"period_end"`
	EnergyWh    int64     `json:"energy_wh"`
	AmountCents int64     `json:"amount_cents"`
	Status      string    `json:"status"`
	Customer    *Account  `json:"customer,omitempty"`
}

type BillLine struct {
	Description string `json:"description"`
	AmountCents int64  `json:"amount_cents"`
}

func Open(dsn string) (*DB, error) {
	db, err := sql.Open("postgres", dsn)
	if err != nil {
		return nil, fmt.Errorf("open db: %w", err)
	}
	db.SetMaxOpenConns(20)
	db.SetConnMaxIdleTime(5 * time.Minute)
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := db.PingContext(ctx); err != nil {
		db.Close()
		return nil, fmt.Errorf("ping db: %w", err)
	}
	return &DB{db}, nil
}

func (d *DB) WithTx(ctx context.Context, fn func(tx *sql.Tx) error) error {
	tx, err := d.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	if err := fn(tx); err != nil {
		tx.Rollback()
		return err
	}
	return tx.Commit()
}
