package store

import (
	"context"
	"database/sql"
	"errors"
	"time"
)

var ErrNotFound = errors.New("not found")

type DB struct {
	sql *sql.DB
}

func New(db *sql.DB) *DB {
	return &DB{sql: db}
}

type Account struct {
	ID           int64
	FleetID      int64
	Role         string
	PasswordHash string
}

type Vehicle struct {
	ID         int64     `json:"id"`
	FleetID    int64     `json:"fleet_id"`
	VIN        string    `json:"vin"`
	Plate      string    `json:"plate"`
	Model      string    `json:"model"`
	OdometerKm int64     `json:"odometer_km"`
	CreatedAt  time.Time `json:"created_at"`
}

type MaintenanceRecord struct {
	ID          int64     `json:"id"`
	VehicleID   int64     `json:"vehicle_id"`
	Kind        string    `json:"kind"`
	PerformedAt time.Time `json:"performed_at"`
	OdometerKm  int64     `json:"odometer_km"`
}

type Trip struct {
	ID         int64      `json:"id"`
	FleetID    int64      `json:"fleet_id"`
	VehicleID  int64      `json:"vehicle_id"`
	DriverID   int64      `json:"driver_id"`
	Status     string     `json:"status"`
	StartedAt  time.Time  `json:"started_at"`
	EndedAt    *time.Time `json:"ended_at,omitempty"`
	DistanceKm float64    `json:"distance_km"`
}

type Geofence struct {
	ID        int64   `json:"id"`
	FleetID   int64   `json:"fleet_id"`
	Name      string  `json:"name"`
	CenterLat float64 `json:"center_lat"`
	CenterLon float64 `json:"center_lon"`
	RadiusM   float64 `json:"radius_m"`
}

func (d *DB) FindAccountByEmail(ctx context.Context, email string) (Account, error) {
	var a Account
	err := d.sql.QueryRowContext(ctx,
		"SELECT id, fleet_id, role, password_hash FROM accounts WHERE lower(email) = lower($1)", email).
		Scan(&a.ID, &a.FleetID, &a.Role, &a.PasswordHash)
	if errors.Is(err, sql.ErrNoRows) {
		return Account{}, ErrNotFound
	}
	return a, err
}
