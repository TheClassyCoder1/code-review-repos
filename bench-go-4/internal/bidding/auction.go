package bidding

import (
	"context"
	"sort"
	"time"

	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/exchange"
)

// Result is the outcome of one auction.
type Result struct {
	Winner         *exchange.BidResponse `json:"winner,omitempty"`
	ClearingMicros int64                 `json:"clearing_price_micros"`
	Bids           int                   `json:"bids"`
}

// Auctioneer runs second-price auctions across external bidders.
type Auctioneer struct {
	client     *exchange.Client
	bidderURLs []string
	timeout    time.Duration
	floor      int64
}

// NewAuctioneer returns an auctioneer with the given per-auction timeout and
// floor price.
func NewAuctioneer(c *exchange.Client, bidderURLs []string, timeout time.Duration, floorMicros int64) *Auctioneer {
	return &Auctioneer{client: c, bidderURLs: bidderURLs, timeout: timeout, floor: floorMicros}
}

// CollectBids asks every bidder in parallel and returns the bids received
// before ctx is done.
func (a *Auctioneer) CollectBids(ctx context.Context, br exchange.BidRequest) []exchange.BidResponse {
	results := make(chan *exchange.BidResponse)
	for _, u := range a.bidderURLs {
		go func(url string) {
			bid, err := a.client.RequestBid(ctx, url, br)
			if err != nil {
				bid = nil
			}
			results <- bid
		}(u)
	}
	var bids []exchange.BidResponse
	for range a.bidderURLs {
		select {
		case bid := <-results:
			if bid != nil {
				bids = append(bids, *bid)
			}
		case <-ctx.Done():
			return bids
		}
	}
	return bids
}

// Run collects bids for br within the auction timeout and picks a winner.
func (a *Auctioneer) Run(ctx context.Context, br exchange.BidRequest) Result {
	ctx, cancel := context.WithTimeout(ctx, a.timeout)
	defer cancel()
	bids := a.CollectBids(ctx, br)
	return RunAuction(bids, a.floor)
}

// RunAuction picks the highest bid at or above the floor. The winner pays
// the second-highest price (or the floor), plus one micro.
func RunAuction(bids []exchange.BidResponse, floorMicros int64) Result {
	eligible := make([]exchange.BidResponse, 0, len(bids))
	for _, b := range bids {
		if b.PriceMicros >= floorMicros {
			eligible = append(eligible, b)
		}
	}
	res := Result{Bids: len(bids)}
	if len(eligible) == 0 {
		return res
	}
	sort.SliceStable(eligible, func(i, j int) bool {
		return eligible[i].PriceMicros > eligible[j].PriceMicros
	})
	winner := eligible[0]
	clearing := floorMicros
	if len(eligible) > 1 {
		clearing = eligible[1].PriceMicros
	}
	if clearing+1 < winner.PriceMicros {
		clearing++
	} else {
		clearing = winner.PriceMicros
	}
	res.Winner = &winner
	res.ClearingMicros = clearing
	return res
}
