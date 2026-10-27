package billing

import (
	"errors"
	"time"

	"meterhub/internal/store"
)

func EnergyCharge(tiers []store.Tier, wattHours int64) int64 {
	var milliCents, floor int64
	remaining := wattHours
	for _, t := range tiers {
		if remaining <= 0 {
			break
		}
		band := remaining
		if t.UpToWh > 0 && t.UpToWh-floor < band {
			band = t.UpToWh - floor
		}
		milliCents += band * t.MilliCentsPerKWh / 1000
		remaining -= band
		floor = t.UpToWh
	}
	return (milliCents + 500) / 1000
}

func ValidateTiers(tiers []store.Tier) error {
	if len(tiers) == 0 {
		return errors.New("tariff needs at least one tier")
	}
	var prev int64
	for i, t := range tiers {
		last := i == len(tiers)-1
		if t.MilliCentsPerKWh < 0 {
			return errors.New("tier rate must not be negative")
		}
		if !last && t.UpToWh <= prev {
			return errors.New("tier bounds must increase")
		}
		if last && t.UpToWh != 0 {
			return errors.New("last tier must be open-ended")
		}
		prev = t.UpToWh
	}
	return nil
}

func StandingCharge(centsPerDay int64, from, to time.Time) int64 {
	if !to.After(from) {
		return 0
	}
	span := to.Sub(from)
	days := int64(span / (24 * time.Hour))
	if span%(24*time.Hour) != 0 {
		days++
	}
	return days * centsPerDay
}

func ExportedEnergy(imported, net int64) int64 {
	if net >= imported {
		return 0
	}
	return imported - net
}

func SolarExportCredit(exportedWh int64, feedInRatePerKWh float64) int64 {
	kwh := float64(exportedWh) / 1000
	credit := kwh * feedInRatePerKWh
	return int64(credit * 100)
}
