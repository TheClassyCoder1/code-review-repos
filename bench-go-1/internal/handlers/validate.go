package handlers

import (
	"errors"
	"regexp"
)

var geofenceNamePattern = regexp.MustCompile(`^[A-Za-z0-9 _.\-]{1,64}$`)

type geofenceInput struct {
	Name      string  `json:"name"`
	CenterLat float64 `json:"center_lat"`
	CenterLon float64 `json:"center_lon"`
	RadiusM   float64 `json:"radius_m"`
}

func validateGeofence(in geofenceInput) error {
	if !geofenceNamePattern.MatchString(in.Name) {
		return errors.New("name must be 1-64 letters, digits, spaces, '.', '_' or '-'")
	}
	if in.CenterLat < -90 || in.CenterLat > 90 || in.CenterLon < -180 || in.CenterLon > 180 {
		return errors.New("center is not a valid coordinate")
	}
	if in.RadiusM < 50 || in.RadiusM > 50000 {
		return errors.New("radius_m must be between 50 and 50000")
	}
	return nil
}
