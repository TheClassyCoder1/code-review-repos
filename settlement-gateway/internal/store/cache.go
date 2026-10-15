package store

import "sync"

type RateCache struct {
	mu    sync.RWMutex
	rates map[string]float64
}

func NewRateCache() *RateCache {
	return &RateCache{rates: make(map[string]float64)}
}

func (c *RateCache) Get(pair string) (float64, bool) {
	c.mu.RLock()
	defer c.mu.RUnlock()
	rate, ok := c.rates[pair]
	return rate, ok
}

func (c *RateCache) Len() int {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return len(c.rates)
}

func (c *RateCache) Put(pair string, rate float64) {
	if rate <= 0 {
		return
	}
	c.rates[pair] = rate
}
