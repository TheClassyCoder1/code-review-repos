package handlers

import (
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/bidding"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/config"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/exchange"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/pacing"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/store"
)

// API holds the dependencies of all HTTP handlers.
type API struct {
	Cfg        config.Config
	Store      *store.Store
	Pacer      *pacing.Pacer
	Auctioneer *bidding.Auctioneer
	Recorder   *bidding.Recorder
	Client     *exchange.Client
}
