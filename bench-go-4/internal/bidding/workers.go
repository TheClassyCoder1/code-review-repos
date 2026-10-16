package bidding

import (
	"context"
	"errors"
	"log"
	"sync"
	"time"

	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/exchange"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/store"
)

// ErrDuplicateImpression is returned by Record for an impression id that was
// already accepted.
var ErrDuplicateImpression = errors.New("duplicate impression")

// ImpressionEvent is an impression reported by the ad server, with the win
// notice to send for it.
type ImpressionEvent struct {
	Impression store.Impression
	Notice     exchange.WinNotice
}

const (
	maxSeen = 100_000
	seenTTL = 10 * time.Minute
)

// Recorder persists impressions and sends win notices on a pool of workers.
type Recorder struct {
	store     *store.Store
	noticeURL string
	jobs      chan ImpressionEvent
	wg        sync.WaitGroup

	mu   sync.Mutex
	seen map[string]time.Time
}

// NewRecorder returns a recorder with a queue of the given size.
func NewRecorder(st *store.Store, noticeURL string, queueSize int) *Recorder {
	return &Recorder{
		store:     st,
		noticeURL: noticeURL,
		jobs:      make(chan ImpressionEvent, queueSize),
		seen:      make(map[string]time.Time),
	}
}

// Start launches n workers that drain the queue.
func (r *Recorder) Start(n int) {
	for i := 0; i < n; i++ {
		r.wg.Add(1)
		go func() {
			defer r.wg.Done()
			for ev := range r.jobs {
				r.process(ev)
			}
		}()
	}
}

// Record queues an impression. It returns ErrDuplicateImpression when the
// impression id was seen recently; otherwise the event is accepted and
// retried by the workers until stored.
func (r *Recorder) Record(ev ImpressionEvent) error {
	r.mu.Lock()
	if _, dup := r.seen[ev.Impression.ID]; dup {
		r.mu.Unlock()
		return ErrDuplicateImpression
	}
	now := time.Now()
	if len(r.seen) >= maxSeen {
		for id, at := range r.seen {
			if now.Sub(at) > seenTTL {
				delete(r.seen, id)
			}
		}
	}
	r.seen[ev.Impression.ID] = now
	r.mu.Unlock()
	r.jobs <- ev
	return nil
}

func (r *Recorder) process(ev ImpressionEvent) {
	backoff := 100 * time.Millisecond
	for attempt := 1; ; attempt++ {
		_, err := r.store.RecordImpression(context.Background(), ev.Impression)
		if err == nil {
			break
		}
		log.Printf("recorder: store impression for campaign %d (attempt %d): %v", ev.Impression.CampaignID, attempt, err)
		time.Sleep(backoff)
		if backoff < 5*time.Second {
			backoff *= 2
		}
	}
	if r.noticeURL == "" {
		return
	}
	if err := exchange.NotifyWin(r.noticeURL, ev.Notice); err != nil {
		log.Printf("recorder: win notice for campaign %d: %v", ev.Notice.CampaignID, err)
	}
}

// Close stops accepting events and waits for the workers to drain the queue.
func (r *Recorder) Close() {
	close(r.jobs)
	r.wg.Wait()
}
