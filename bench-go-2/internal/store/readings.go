package store

import (
	"context"
	"database/sql"
	"time"
)

func (d *DB) InsertReadings(ctx context.Context, rs []Reading) (int64, error) {
	var inserted int64
	err := d.WithTx(ctx, func(tx *sql.Tx) error {
		for _, r := range rs {
			res, err := tx.ExecContext(ctx,
				`INSERT INTO readings (meter_id, read_at, watt_hours) VALUES ($1, $2, $3)
				 ON CONFLICT (meter_id, read_at) DO NOTHING`, r.MeterID, r.ReadAt, r.WattHours)
			if err != nil {
				return err
			}
			n, _ := res.RowsAffected()
			inserted += n
			if _, err := tx.ExecContext(ctx,
				`UPDATE meters SET last_reading_at = GREATEST(COALESCE(last_reading_at, $2), $2) WHERE id = $1`,
				r.MeterID, r.ReadAt); err != nil {
				return err
			}
		}
		return nil
	})
	return inserted, err
}

func (d *DB) CountReadings(ctx context.Context, meterID int64, from, to time.Time) (int64, error) {
	var n int64
	err := d.QueryRowContext(ctx,
		`SELECT COUNT(*) FROM readings WHERE meter_id = $1 AND read_at >= $2 AND read_at < $3`,
		meterID, from, to).Scan(&n)
	return n, err
}

func (d *DB) ReadingsForPeriod(ctx context.Context, meterID int64, from, to time.Time) ([]Reading, error) {
	rows, err := d.QueryContext(ctx,
		`SELECT meter_id, read_at, watt_hours FROM readings
		 WHERE meter_id = $1 AND read_at >= $2 AND read_at < $3 ORDER BY read_at`,
		meterID, from, to)
	if err != nil {
		return nil, err
	}
	var out []Reading
	for rows.Next() {
		var r Reading
		if err := rows.Scan(&r.MeterID, &r.ReadAt, &r.WattHours); err != nil {
			return nil, err
		}
		out = append(out, r)
	}
	return out, rows.Err()
}

func (d *DB) LatestReadings(ctx context.Context, meterID int64, limit int) ([]Reading, error) {
	rows, err := d.QueryContext(ctx,
		`SELECT meter_id, read_at, watt_hours FROM (
		   SELECT meter_id, read_at, watt_hours FROM readings WHERE meter_id = $1
		   ORDER BY read_at DESC LIMIT $2) recent ORDER BY read_at`, meterID, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Reading{}
	for rows.Next() {
		var r Reading
		if err := rows.Scan(&r.MeterID, &r.ReadAt, &r.WattHours); err != nil {
			return nil, err
		}
		out = append(out, r)
	}
	return out, rows.Err()
}

func (d *DB) StreamReadings(ctx context.Context, meterID int64, from, to time.Time) (*sql.Rows, error) {
	rows, err := d.QueryContext(ctx,
		`SELECT meter_id, read_at, watt_hours FROM readings
		 WHERE meter_id = $1 AND read_at >= $2 AND read_at < $3 ORDER BY read_at`,
		meterID, from, to)
	if err != nil {
		return nil, err
	}
	return rows, nil
}
