package store

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
)

const vehicleColumns = "id, fleet_id, vin, plate, model, odometer_km, created_at"

func scanVehicle(row scanner) (Vehicle, error) {
	var v Vehicle
	err := row.Scan(&v.ID, &v.FleetID, &v.VIN, &v.Plate, &v.Model, &v.OdometerKm, &v.CreatedAt)
	return v, err
}

func (d *DB) GetVehicle(ctx context.Context, id int64) (Vehicle, error) {
	row := d.sql.QueryRowContext(ctx, "SELECT "+vehicleColumns+" FROM vehicles WHERE id = $1", id)
	v, err := scanVehicle(row)
	if errors.Is(err, sql.ErrNoRows) {
		return Vehicle{}, ErrNotFound
	}
	if err != nil {
		return Vehicle{}, fmt.Errorf("get vehicle %d: %w", id, err)
	}
	return v, nil
}

func (d *DB) VehicleFleet(ctx context.Context, id int64) (int64, error) {
	var fleetID int64
	err := d.sql.QueryRowContext(ctx, "SELECT fleet_id FROM vehicles WHERE id = $1", id).Scan(&fleetID)
	if errors.Is(err, sql.ErrNoRows) {
		return 0, ErrNotFound
	}
	return fleetID, err
}

func (d *DB) ListVehicles(ctx context.Context, fleetID int64, sortColumn string) ([]Vehicle, error) {
	q := fmt.Sprintf("SELECT %s FROM vehicles WHERE fleet_id = $1 ORDER BY %s", vehicleColumns, sortColumn)
	rows, err := d.sql.QueryContext(ctx, q, fleetID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Vehicle{}
	for rows.Next() {
		v, err := scanVehicle(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, v)
	}
	return out, rows.Err()
}

func (d *DB) ListMaintenance(ctx context.Context, vehicleID int64) ([]MaintenanceRecord, error) {
	rows, err := d.sql.QueryContext(ctx,
		"SELECT id, vehicle_id, kind, performed_at, odometer_km FROM maintenance WHERE vehicle_id = $1 ORDER BY performed_at DESC",
		vehicleID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []MaintenanceRecord{}
	for rows.Next() {
		var m MaintenanceRecord
		if err := rows.Scan(&m.ID, &m.VehicleID, &m.Kind, &m.PerformedAt, &m.OdometerKm); err != nil {
			return nil, err
		}
		out = append(out, m)
	}
	return out, rows.Err()
}

func (d *DB) SaveDeviceTokenHash(ctx context.Context, vehicleID int64, tokenHash string) error {
	_, err := d.sql.ExecContext(ctx,
		"INSERT INTO device_tokens (vehicle_id, token_hash) VALUES ($1, $2) ON CONFLICT (vehicle_id) DO UPDATE SET token_hash = EXCLUDED.token_hash, rotated_at = now()",
		vehicleID, tokenHash)
	return err
}

func (d *DB) InsertGeofence(ctx context.Context, g Geofence) (int64, error) {
	var id int64
	err := d.sql.QueryRowContext(ctx,
		"INSERT INTO geofences (fleet_id, name, center_lat, center_lon, radius_m) VALUES ($1, $2, $3, $4, $5) RETURNING id",
		g.FleetID, g.Name, g.CenterLat, g.CenterLon, g.RadiusM).Scan(&id)
	if err != nil {
		return 0, fmt.Errorf("insert geofence: %w", err)
	}
	return id, nil
}

func (d *DB) GetGeofence(ctx context.Context, id int64) (Geofence, error) {
	var g Geofence
	err := d.sql.QueryRowContext(ctx,
		"SELECT id, fleet_id, name, center_lat, center_lon, radius_m FROM geofences WHERE id = $1", id).
		Scan(&g.ID, &g.FleetID, &g.Name, &g.CenterLat, &g.CenterLon, &g.RadiusM)
	if errors.Is(err, sql.ErrNoRows) {
		return Geofence{}, ErrNotFound
	}
	return g, err
}

func (d *DB) ListGeofences(ctx context.Context, fleetID int64) ([]Geofence, error) {
	rows, err := d.sql.QueryContext(ctx,
		"SELECT id, fleet_id, name, center_lat, center_lon, radius_m FROM geofences WHERE fleet_id = $1 ORDER BY name", fleetID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Geofence{}
	for rows.Next() {
		var g Geofence
		if err := rows.Scan(&g.ID, &g.FleetID, &g.Name, &g.CenterLat, &g.CenterLon, &g.RadiusM); err != nil {
			return nil, err
		}
		out = append(out, g)
	}
	return out, rows.Err()
}
