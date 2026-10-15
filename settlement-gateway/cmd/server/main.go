package main

import (
	"database/sql"
	"log"
	"net/http"

	_ "github.com/lib/pq"

	"github.com/theclassycoder1/code-review-repos/settlement-gateway/internal/client"
	"github.com/theclassycoder1/code-review-repos/settlement-gateway/internal/config"
	"github.com/theclassycoder1/code-review-repos/settlement-gateway/internal/export"
	"github.com/theclassycoder1/code-review-repos/settlement-gateway/internal/handlers"
	"github.com/theclassycoder1/code-review-repos/settlement-gateway/internal/store"
	"github.com/theclassycoder1/code-review-repos/settlement-gateway/internal/token"
)

func main() {
	cfg := config.Load()
	http.DefaultClient.Timeout = cfg.HTTPTimeout

	db, err := sql.Open("postgres", cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("open database: %v", err)
	}
	defer db.Close()

	signer := token.NewSigner(cfg.SigningKey)
	h := handlers.New(
		store.NewSettlementStore(db),
		store.NewRateCache(),
		store.NewTotals(),
		client.NewBankClient(cfg.BankBaseURL),
		client.NewFXClient(cfg.FXBaseURL),
		export.NewArchiver(cfg.StatementDir),
	)

	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", h.Health)
	mux.HandleFunc("GET /settlements", h.ListSettlements)
	mux.HandleFunc("POST /settlements", h.CreateSettlement)
	mux.HandleFunc("GET /settlements/{id}", h.GetSettlement)
	mux.Handle("POST /settlements/{id}/approve", handlers.RequireRole("ops", http.HandlerFunc(h.ApproveSettlement)))
	mux.HandleFunc("GET /payouts/{id}/status", h.GetPayoutStatus)
	mux.HandleFunc("GET /totals", h.GetTotals)
	mux.HandleFunc("GET /statements/{id}", h.DownloadStatement)
	mux.HandleFunc("POST /statements/{id}/archive", h.ArchiveStatement)
	mux.HandleFunc("GET /rates/{pair}", h.GetRate)
	mux.HandleFunc("POST /rates/{pair}/refresh", h.RefreshRate)

	log.Printf("settlement-gateway listening on %s", cfg.Addr)
	log.Fatal(http.ListenAndServe(cfg.Addr, handlers.Authenticate(signer, mux)))
}
