package ingest

import (
	"context"
	"fmt"
	"log"
	"strconv"
	"strings"
	"sync"
	"time"

	"meterhub/internal/billing"
	"meterhub/internal/store"
)

type Stats struct {
	Batches   int64     `json:"batches"`
	Readings  int64     `json:"readings"`
	LastBatch time.Time `json:"last_batch"`
}

type Service struct {
	db       *store.DB
	gw       *GatewayClient
	gateways []string
	workers  int

	mu    sync.Mutex
	stats Stats
}

func NewService(db *store.DB, gw *GatewayClient, gateways string, workers int) *Service {
	return &Service{db: db, gw: gw, gateways: gatewayList(gateways), workers: workers}
}

func ParseReadingLine(line string) (store.Reading, error) {
	var r store.Reading
	parts := strings.Split(strings.TrimSpace(line), ",")
	if len(parts) != 3 {
		return r, fmt.Errorf("expected 3 fields, got %d", len(parts))
	}
	id, err := strconv.ParseInt(strings.TrimSpace(parts[0]), 10, 64)
	if err != nil || id <= 0 {
		return r, fmt.Errorf("invalid meter id %q", parts[0])
	}
	at, err := time.Parse(time.RFC3339, strings.TrimSpace(parts[1]))
	if err != nil {
		return r, fmt.Errorf("invalid timestamp: %w", err)
	}
	wh, err := strconv.ParseInt(strings.TrimSpace(parts[2]), 10, 64)
	if err != nil || wh < 0 {
		return r, fmt.Errorf("invalid watt-hours %q", parts[2])
	}
	return store.Reading{MeterID: id, ReadAt: at.UTC(), WattHours: wh}, nil
}

func (s *Service) Run(ctx context.Context, interval time.Duration) {
	t := time.NewTicker(interval)
	defer t.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-t.C:
			if err := s.syncOnce(ctx); err != nil {
				log.Printf("ingest: sync failed: %v", err)
			}
		}
	}
}

func (s *Service) syncOnce(ctx context.Context) error {
	batches, err := s.gw.PullAll(ctx, s.gateways)
	if err != nil {
		return err
	}
	sem := make(chan struct{}, s.workers)
	var wg sync.WaitGroup
	for _, b := range batches {
		wg.Add(1)
		sem <- struct{}{}
		go func(b Batch) {
			defer wg.Done()
			defer func() { <-sem }()
			if err := s.processBatch(ctx, b); err != nil {
				log.Printf("ingest: batch=%s: %v", b.ID, err)
			}
		}(b)
	}
	wg.Wait()
	return nil
}

func (s *Service) processBatch(ctx context.Context, b Batch) error {
	inserted, err := s.db.InsertReadings(ctx, b.Readings)
	if err != nil {
		return fmt.Errorf("store batch: %w", err)
	}
	if inserted > 0 {
		s.chargePrepaid(ctx, b.Readings)
	}
	return s.finishBatch(ctx, b, inserted)
}

func (s *Service) chargePrepaid(ctx context.Context, rs []store.Reading) {
	byMeter := map[int64][]store.Reading{}
	for _, r := range rs {
		byMeter[r.MeterID] = append(byMeter[r.MeterID], r)
	}
	for meterID, mrs := range byMeter {
		used := billing.TotalWh(billing.ConsumptionDeltas(mrs))
		t, err := s.db.CurrentTariff(ctx, meterID, mrs[0].ReadAt)
		if err != nil {
			log.Printf("ingest: meter=%d: no tariff: %v", meterID, err)
			continue
		}
		accountID, err := s.db.AccountForMeter(ctx, meterID)
		if err != nil {
			continue
		}
		if _, err := s.db.DeductUsage(ctx, accountID, billing.EnergyCharge(t.Tiers, used)); err != nil {
			log.Printf("ingest: meter=%d: deduct: %v", meterID, err)
		}
	}
}

func (s *Service) Stats() Stats {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.stats
}

func (s *Service) finishBatch(ctx context.Context, b Batch, inserted int64) error {
	if err := s.gw.Ack(ctx, b.ID); err != nil {
		log.Printf("ingest: ack batch=%s failed: %v", b.ID, err)
	}
	s.mu.Lock()
	s.stats.Batches++
	s.stats.Readings += inserted
	s.stats.LastBatch = time.Now()
	s.mu.Unlock()
	return nil
}
