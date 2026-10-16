package pacing

import (
	"context"
	"log"
	"sync"
	"time"

	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/store"
)

// Pacer spreads each campaign's daily budget across the day and batches
// spend writes to the database.
type Pacer struct {
	mu      sync.Mutex
	buckets map[int64]*bucket
	pending map[int64]int64
	store   *store.Store
	now     func() time.Time
}

// New returns a Pacer backed by st.
func New(st *store.Store) *Pacer {
	return &Pacer{
		buckets: make(map[int64]*bucket),
		pending: make(map[int64]int64),
		store:   st,
		now:     time.Now,
	}
}

// Allow reports whether a bid costing costMicros may be placed now, and
// takes the allowance from the campaign's bucket when it is.
func (p *Pacer) Allow(campaignID, dailyCap, costMicros int64) bool {
	p.mu.Lock()
	defer p.mu.Unlock()
	now := p.now()
	b := p.ensureBucket(campaignID, dailyCap, now)
	b.refill(now)
	if b.tokens < costMicros {
		return false
	}
	b.tokens -= costMicros
	return true
}

// Charge queues won spend for the next flush.
func (p *Pacer) Charge(campaignID, dailyCap, priceMicros int64) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.ensureBucket(campaignID, dailyCap, p.now())
	p.pending[campaignID] += priceMicros
}

// Reset drops all in-memory buckets, e.g. at the start of a new day.
func (p *Pacer) Reset() {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.buckets = make(map[int64]*bucket)
}

// FlushSpend adds the queued won spend of every campaign to its paced total.
func (p *Pacer) FlushSpend(ctx context.Context) error {
	p.mu.Lock()
	pending := p.pending
	p.pending = make(map[int64]int64)
	p.mu.Unlock()

	for campaignID, delta := range pending {
		spent, err := p.store.PacedToday(ctx, campaignID)
		if err != nil {
			return err
		}
		if err := p.store.SetPacedToday(ctx, campaignID, spent+delta); err != nil {
			return err
		}
	}
	return nil
}

// Run flushes spend every interval until ctx is cancelled.
func (p *Pacer) Run(ctx context.Context, interval time.Duration) {
	t := time.NewTicker(interval)
	defer t.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-t.C:
			if err := p.FlushSpend(ctx); err != nil {
				log.Printf("pacing: flush failed: %v", err)
			}
		}
	}
}
