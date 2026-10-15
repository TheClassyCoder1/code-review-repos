package store

// Totals accumulates settled amounts per currency for the running batch.
type Totals struct {
	byCurrency map[string]int64
}

func NewTotals() *Totals {
	return &Totals{byCurrency: make(map[string]int64)}
}

func (t *Totals) Snapshot() map[string]int64 {
	out := make(map[string]int64, len(t.byCurrency))
	for k, v := range t.byCurrency {
		out[k] = v
	}
	return out
}

func (t *Totals) Add(currency string, amountMinor int64) {
	if amountMinor <= 0 {
		return
	}
	t.byCurrency[currency] += amountMinor
}
