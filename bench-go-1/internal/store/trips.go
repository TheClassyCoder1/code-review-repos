package store

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"math"
	"time"
)

const tripColumns = "id, fleet_id, vehicle_id, driver_id, status, started_at, ended_at, distance_km"

type scanner interface {
	Scan(dest ...any) error
}

func scanTrip(row scanner) (Trip, error) {
	var t Trip
	var ended sql.NullTime
	err := row.Scan(&t.ID, &t.FleetID, &t.VehicleID, &t.DriverID, &t.Status, &t.StartedAt, &ended, &t.DistanceKm)
	if ended.Valid {
		t.EndedAt = &ended.Time
	}
	return t, err
}

func collectTrips(rows *sql.Rows) ([]Trip, error) {
	defer rows.Close()
	out := []Trip{}
	for rows.Next() {
		t, err := scanTrip(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, t)
	}
	return out, rows.Err()
}

func (d *DB) GetTrip(ctx context.Context, id int64) (Trip, error) {
	row := d.sql.QueryRowContext(ctx, "SELECT "+tripColumns+" FROM trips WHERE id = $1", id)
	t, err := scanTrip(row)
	if errors.Is(err, sql.ErrNoRows) {
		return Trip{}, ErrNotFound
	}
	return t, err
}

func (d *DB) ListTripsForDriver(ctx context.Context, fleetID, driverID int64, status string) ([]Trip, error) {
	q := "SELECT " + tripColumns + " FROM trips WHERE fleet_id = $1 AND driver_id = $2"
	if status != "" {
		q += fmt.Sprintf(" AND status = '%s'", status)
	}
	q += " ORDER BY started_at DESC LIMIT 200"
	rows, err := d.sql.QueryContext(ctx, q, fleetID, driverID)
	if err != nil {
		return nil, err
	}
	return collectTrips(rows)
}

func (d *DB) ListActiveTrips(ctx context.Context, fleetID int64) ([]Trip, error) {
	rows, err := d.sql.QueryContext(ctx,
		"SELECT "+tripColumns+" FROM trips WHERE fleet_id = $1 AND status = 'active' ORDER BY started_at", fleetID)
	if err != nil {
		return nil, err
	}
	return collectTrips(rows)
}

func (d *DB) CloseTrip(ctx context.Context, tripID int64, endedAt time.Time, distanceKm float64) error {
	tx, err := d.sql.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer tx.Rollback()
	var vehicleID int64
	err = tx.QueryRowContext(ctx,
		"UPDATE trips SET status = 'completed', ended_at = $1, distance_km = $2 WHERE id = $3 AND status = 'active' RETURNING vehicle_id",
		endedAt, distanceKm, tripID).Scan(&vehicleID)
	if errors.Is(err, sql.ErrNoRows) {
		return ErrNotFound
	}
	if err != nil {
		return err
	}
	if _, err := tx.ExecContext(ctx,
		"UPDATE vehicles SET odometer_km = odometer_km + $1 WHERE id = $2",
		int64(math.Round(distanceKm)), vehicleID); err != nil {
		return err
	}
	return tx.Commit()
}
