package geo

import (
	"bytes"
	"encoding/gob"
	"fmt"
	"math"
	"time"
)

const earthRadiusKm = 6371.0

type Point struct {
	Lat float64
	Lon float64
	At  time.Time
}

type Route struct {
	TripID int64
	Points []Point
}

func radians(deg float64) float64 {
	return deg * math.Pi / 180
}

func HaversineKm(a, b Point) float64 {
	dLat := radians(b.Lat - a.Lat)
	dLon := radians(b.Lon - a.Lon)
	h := math.Sin(dLat/2)*math.Sin(dLat/2) +
		math.Cos(radians(a.Lat))*math.Cos(radians(b.Lat))*math.Sin(dLon/2)*math.Sin(dLon/2)
	return 2 * earthRadiusKm * math.Asin(math.Min(1, math.Sqrt(h)))
}

type RouteSummary struct {
	DistanceKm  float64       `json:"distance_km"`
	MaxSpeedKmh float64       `json:"max_speed_kmh"`
	Duration    time.Duration `json:"duration"`
}

func SummarizeRoute(points []Point) RouteSummary {
	var s RouteSummary
	if len(points) < 2 {
		return s
	}
	for i := 1; i < len(points); i++ {
		d := HaversineKm(points[i-1], points[i])
		s.DistanceKm += d
		if dt := points[i].At.Sub(points[i-1].At).Hours(); dt > 0 {
			if v := d / dt; v > s.MaxSpeedKmh {
				s.MaxSpeedKmh = v
			}
		}
	}
	s.Duration = points[len(points)-1].At.Sub(points[0].At)
	return s
}

func EncodeRoute(r Route) ([]byte, error) {
	var buf bytes.Buffer
	if err := gob.NewEncoder(&buf).Encode(r); err != nil {
		return nil, fmt.Errorf("encode route: %w", err)
	}
	return buf.Bytes(), nil
}

func DecodeRoute(blob []byte) (Route, error) {
	var r Route
	if err := gob.NewDecoder(bytes.NewReader(blob)).Decode(&r); err != nil {
		return Route{}, fmt.Errorf("decode route: %w", err)
	}
	return r, nil
}
