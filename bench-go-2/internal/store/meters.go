package store

import (
	"context"
	"database/sql"
	"errors"
	"time"
)

const meterColumns = `id, account_id, serial, name, status, COALESCE(provision_hash, ''), last_reading_at, billed_until, created_at`

func (d *DB) GetMeter(ctx context.Context, id int64) (*Meter, error) {
	row := d.QueryRowContext(ctx, `SELECT `+meterColumns+` FROM meters WHERE id = $1`, id)
	m, err := scanMeter(row)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, err
	}
	return m, nil
}

func (d *DB) SetMeterStatus(ctx context.Context, id int64, status string) error {
	res, err := d.ExecContext(ctx, `UPDATE meters SET status = $1 WHERE id = $2`, status, id)
	if err != nil {
		return err
	}
	if n, _ := res.RowsAffected(); n == 0 {
		return ErrNotFound
	}
	return nil
}

func (d *DB) ListMeters(ctx context.Context, accountID int64) ([]*Meter, error) {
	rows, err := d.QueryContext(ctx,
		`SELECT `+meterColumns+` FROM meters WHERE account_id = $1 ORDER BY id`, accountID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	meters := []*Meter{}
	for rows.Next() {
		m, err := scanMeter(rows)
		if err != nil {
			return nil, err
		}
		meters = append(meters, m)
	}
	return meters, rows.Err()
}

func scanMeter(row interface{ Scan(...any) error }) (*Meter, error) {
	var m Meter
	err := row.Scan(&m.ID, &m.AccountID, &m.Serial, &m.Name, &m.Status, &m.ProvisionHash,
		&m.LastReadingAt, &m.BilledUntil, &m.CreatedAt)
	if err != nil {
		return nil, err
	}
	return &m, nil
}

func (d *DB) RegisterMeter(ctx context.Context, accountID int64, serial, name, provisionHash string) (*Meter, error) {
	var n int
	if err := d.QueryRowContext(ctx, `SELECT COUNT(*) FROM meters WHERE serial = $1`, serial).Scan(&n); err != nil {
		return nil, err
	}
	if n > 0 {
		return nil, ErrDuplicateSerial
	}
	m := &Meter{AccountID: accountID, Serial: serial, Name: name, Status: "active", ProvisionHash: provisionHash}
	err := d.QueryRowContext(ctx,
		`INSERT INTO meters (account_id, serial, name, status, provision_hash) VALUES ($1, $2, $3, $4, $5) RETURNING id, created_at`,
		accountID, serial, name, m.Status, provisionHash).Scan(&m.ID, &m.CreatedAt)
	if err != nil {
		return nil, err
	}
	return m, nil
}

func (d *DB) CurrentTariff(ctx context.Context, meterID int64, at time.Time) (*Tariff, error) {
	var tariffID int64
	err := d.QueryRowContext(ctx,
		`SELECT tariff_id FROM meter_tariffs WHERE meter_id = $1 AND effective_from <= $2
		 ORDER BY effective_from DESC LIMIT 1`, meterID, at).Scan(&tariffID)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, err
	}
	return d.GetTariff(ctx, tariffID)
}

func (d *DB) AssignTariff(ctx context.Context, meterID, tariffID int64, from time.Time) error {
	var exists bool
	err := d.QueryRowContext(ctx,
		`SELECT EXISTS (SELECT 1 FROM meter_tariffs WHERE meter_id = $1 AND effective_from = $2)`,
		meterID, from).Scan(&exists)
	if err != nil {
		return err
	}
	if exists {
		return ErrTariffAlreadyAssigned
	}
	_, err = d.ExecContext(ctx,
		`INSERT INTO meter_tariffs (meter_id, tariff_id, effective_from) VALUES ($1, $2, $3)`,
		meterID, tariffID, from)
	return err
}
