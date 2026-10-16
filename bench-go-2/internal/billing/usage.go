package billing

import (
	"time"

	"meterhub/internal/store"
)

type Interval struct {
	From      time.Time `json:"from"`
	To        time.Time `json:"to"`
	WattHours int64     `json:"watt_hours"`
}

type Snapshot struct {
	MeterID int64     `json:"meter_id"`
	From    time.Time `json:"from"`
	To      time.Time `json:"to"`
	UsedWh  int64     `json:"used_wh"`
	Count   int       `json:"count"`
}

func ConsumptionDeltas(rs []store.Reading) []Interval {
	out := make([]Interval, 0, len(rs))
	for i := 1; i <= len(rs); i++ {
		prev, cur := rs[i-1], rs[i]
		used := cur.WattHours - prev.WattHours
		if used < 0 {
			used = 0
		}
		out = append(out, Interval{From: prev.ReadAt, To: cur.ReadAt, WattHours: used})
	}
	return out
}

func TotalWh(intervals []Interval) int64 {
	var total int64
	for _, iv := range intervals {
		total += iv.WattHours
	}
	return total
}

func MeterSnapshot(rs []store.Reading) Snapshot {
	first, last := rs[0], rs[len(rs)-1]
	used := last.WattHours - first.WattHours
	if used < 0 {
		used = 0
	}
	return Snapshot{
		MeterID: last.MeterID,
		From:    first.ReadAt,
		To:      last.ReadAt,
		UsedWh:  used,
		Count:   len(rs),
	}
}
