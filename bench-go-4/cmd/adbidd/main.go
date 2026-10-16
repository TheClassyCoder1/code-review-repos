package main

import (
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/bidding"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/config"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/exchange"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/handlers"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/httpx"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/pacing"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/store"
	"github.com/jackc/pgx/v5/pgxpool"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("config: %v", err)
	}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	pool, err := pgxpool.New(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("postgres: %v", err)
	}
	defer pool.Close()

	st := store.New(pool)
	pacer := pacing.New(st)
	client := exchange.NewBidderClient()
	recorder := bidding.NewRecorder(st, cfg.WinNoticeURL, cfg.QueueSize)
	recorder.Start(cfg.Workers)
	defer recorder.Close()
	go pacer.Run(ctx, cfg.FlushInterval)

	api := &handlers.API{
		Cfg:        cfg,
		Store:      st,
		Pacer:      pacer,
		Auctioneer: bidding.NewAuctioneer(client, cfg.BidderURLs, cfg.BidTimeout, 10_000),
		Recorder:   recorder,
		Client:     client,
	}

	auth := func(h http.HandlerFunc) http.Handler { return httpx.RequireAdvertiser(st, h) }
	withCampaign := func(h http.HandlerFunc) http.Handler {
		return httpx.RequireAdvertiser(st, httpx.LoadCampaign(st, h))
	}

	mux := http.NewServeMux()
	mux.Handle("POST /bid", http.MaxBytesHandler(http.HandlerFunc(api.HandleBidRequest), cfg.MaxBidBytes))
	mux.HandleFunc("POST /impressions", api.TrackImpression)
	mux.HandleFunc(handlers.ConversionRoute, api.ReceiveConversion)

	mux.Handle("POST /campaigns", auth(api.CreateCampaign))
	mux.Handle("GET /campaigns/{id}", auth(api.GetCampaign))
	mux.Handle("DELETE /campaigns/{id}", auth(api.ArchiveCampaign))
	mux.Handle("GET /campaigns/{id}/pacing", auth(api.GetCampaignPacing))
	mux.Handle("POST /campaigns/{id}/pause", auth(api.PauseCampaign))
	mux.Handle("POST /campaigns/{id}/budget/increase", auth(api.IncreaseBudget))
	mux.Handle("GET /campaigns/{id}/bidding", withCampaign(api.GetBidSettings))
	mux.Handle("GET /campaigns/{id}/creative", auth(api.PreviewCreative))
	mux.Handle("GET /campaigns/{id}/impressions", auth(api.ListImpressions))
	mux.Handle("GET /campaigns/{id}/impressions.csv", auth(api.ExportImpressions))
	mux.Handle("GET /campaigns/{id}/spend", auth(api.SpendSummary))
	mux.Handle("POST /invoices/preview", auth(api.InvoicePreview))
	mux.Handle("POST /admin/pacing/reset", httpx.AdminDigest(http.HandlerFunc(api.ResetPacing)))

	srv := &http.Server{
		Addr:              cfg.Addr,
		Handler:           mux,
		ReadHeaderTimeout: 2 * time.Second,
		ReadTimeout:       cfg.ReadTimeout,
		WriteTimeout:      cfg.WriteTimeout,
		IdleTimeout:       60 * time.Second,
	}
	go func() {
		<-ctx.Done()
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
		defer cancel()
		if err := srv.Shutdown(shutdownCtx); err != nil {
			log.Printf("shutdown: %v", err)
		}
	}()
	log.Printf("adbidd listening on %s", cfg.Addr)
	if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Fatalf("serve: %v", err)
	}
}
