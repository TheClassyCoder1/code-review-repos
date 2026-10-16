package billing

import (
	"context"
	"log"
	"time"

	"meterhub/internal/store"
)

type Recalculator struct {
	db      *store.DB
	timeout time.Duration
}

func NewRecalculator(db *store.DB, timeout time.Duration) *Recalculator {
	return &Recalculator{db: db, timeout: timeout}
}

func (r *Recalculator) Recalculate(meterID int64) {
	ctx, cancel := context.WithTimeout(context.Background(), r.timeout)
	defer cancel()
	m, err := r.db.GetMeter(ctx, meterID)
	if err != nil {
		log.Printf("recalc meter=%d: load meter: %v", meterID, err)
		return
	}
	if m.BilledUntil == nil {
		return
	}
	to := time.Now().UTC().Truncate(24 * time.Hour)
	b, lines, err := r.BuildBill(ctx, m, *m.BilledUntil, to)
	if err != nil {
		log.Printf("recalc meter=%d: build bill: %v", meterID, err)
		return
	}
	log.Printf("recalc meter=%d: estimate %d cents over %d lines", meterID, b.AmountCents, len(lines))
}

func (r *Recalculator) BuildBill(ctx context.Context, m *store.Meter, from, to time.Time) (*store.Bill, []store.BillLine, error) {
	t, err := r.db.CurrentTariff(ctx, m.ID, from)
	if err != nil {
		return nil, nil, err
	}
	rs, err := r.db.ReadingsForPeriod(ctx, m.ID, from, to)
	if err != nil {
		return nil, nil, err
	}
	used := TotalWh(ConsumptionDeltas(rs))
	energy := EnergyCharge(t.Tiers, used)
	standing := StandingCharge(t.StandingCentsPerDay, from, to)
	return &store.Bill{
		MeterID:     m.ID,
		AccountID:   m.AccountID,
		PeriodStart: from,
		PeriodEnd:   to,
		EnergyWh:    used,
		AmountCents: energy + standing,
		Status:      "draft",
	}, BuildLines(t, energy, standing), nil
}
