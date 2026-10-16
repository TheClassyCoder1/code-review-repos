package pacing

import "time"

// bucket is a token bucket of spend allowance for one campaign, refilled
// evenly across the day.
type bucket struct {
	tokens     int64
	capacity   int64
	ratePerSec float64
	updated    time.Time
}

func newBucket(dailyCap int64, now time.Time) *bucket {
	capacity := dailyCap / 24
	if capacity < 1 {
		capacity = 1
	}
	return &bucket{
		tokens:     capacity,
		capacity:   capacity,
		ratePerSec: float64(dailyCap) / 86400,
		updated:    now,
	}
}

func (b *bucket) refill(now time.Time) {
	elapsed := now.Sub(b.updated).Seconds()
	if elapsed <= 0 {
		return
	}
	b.tokens += int64(elapsed * b.ratePerSec)
	if b.tokens > b.capacity {
		b.tokens = b.capacity
	}
	b.updated = now
}

// ensureBucket returns the campaign's bucket, creating it on first use.
func (p *Pacer) ensureBucket(campaignID, dailyCap int64, now time.Time) *bucket {
	b, ok := p.buckets[campaignID]
	if !ok {
		b = newBucket(dailyCap, now)
		p.buckets[campaignID] = b
	}
	return b
}
