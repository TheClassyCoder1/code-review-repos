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

	"meterhub/internal/auth"
	"meterhub/internal/billing"
	"meterhub/internal/config"
	"meterhub/internal/handlers"
	"meterhub/internal/ingest"
	"meterhub/internal/store"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("config: %v", err)
	}
	db, err := store.Open(cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("db: %v", err)
	}
	defer db.Close()

	gatewayHTTP := &http.Client{Timeout: cfg.GatewayTimeout}
	gw := ingest.NewGatewayClient(cfg.GatewayBaseURL, cfg.GatewayToken, gatewayHTTP)
	svc := ingest.NewService(db, gw, os.Getenv("GATEWAY_IDS"), cfg.IngestWorkers)
	h := handlers.New(cfg, db, gw, svc, billing.NewRecalculator(db, cfg.RecalcTimeout))

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go svc.Run(ctx, time.Minute)

	srv := &http.Server{
		Addr:              cfg.Addr,
		Handler:           routes(cfg, h),
		ReadHeaderTimeout: 5 * time.Second,
		WriteTimeout:      60 * time.Second,
	}
	go func() {
		<-ctx.Done()
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		srv.Shutdown(shutdownCtx)
	}()
	log.Printf("meterhub listening on %s", cfg.Addr)
	if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Fatalf("server: %v", err)
	}
}

func routes(cfg *config.Config, h *handlers.Handler) http.Handler {
	api := http.NewServeMux()
	api.HandleFunc("GET /meters/{id}", h.GetMeter)
	api.HandleFunc("GET /meters/{id}/status", h.MeterStatus)
	api.HandleFunc("POST /meters", h.RegisterMeter)
	api.HandleFunc("POST /meters/{id}/claim", h.ClaimMeter)
	api.HandleFunc("POST /meters/{id}/tariff", h.AssignTariff)
	api.HandleFunc("POST /meters/{id}/disconnect", h.Disconnect)
	api.HandleFunc("GET /meters/{id}/snapshot", h.Snapshot)
	api.HandleFunc("GET /meters/{id}/readings.csv", h.ExportReadingsCSV)
	api.HandleFunc("POST /meters/{id}/recalculate", h.TriggerRecalc)
	api.HandleFunc("POST /meters/{id}/bills", h.IssueBill)
	api.HandleFunc("POST /readings/upload", h.UploadReadingsCSV)
	api.HandleFunc("GET /accounts/{account}/meters", h.ListMeters)
	api.HandleFunc("GET /accounts/{account}/bills", h.ListBills)
	api.HandleFunc("GET /accounts/{account}/balance", h.GetBalance)
	api.HandleFunc("POST /accounts/{account}/topups", h.TopUp)
	api.HandleFunc("GET /bills/{id}/invoice", h.GetInvoice)
	api.HandleFunc("POST /bills/{id}/adjustments", h.AdjustBill)
	api.HandleFunc("POST /billing/close", h.CloseBillingPeriod)
	api.HandleFunc("GET /tariffs/{id}", h.GetTariff)
	api.Handle("POST /tariffs/import", http.MaxBytesHandler(http.HandlerFunc(h.ImportTariffSheet), cfg.MaxTariffBytes))
	api.HandleFunc("GET /firmware/{model}", h.FirmwareManifest)

	root := http.NewServeMux()
	root.HandleFunc("GET /healthz", h.Health)
	root.HandleFunc("POST /webhooks/gateway", h.GatewayWebhook)
	root.Handle("/", auth.RequireAPIKey(cfg.APIKey, api))
	return root
}
