package main

import (
	"database/sql"
	"log"
	"net/http"
	"time"

	"fleettrack/internal/auth"
	"fleettrack/internal/config"
	"fleettrack/internal/handlers"
	"fleettrack/internal/integrations"
	"fleettrack/internal/store"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("config: %v", err)
	}
	if err := cfg.Validate(); err != nil {
		log.Fatalf("config: %v", err)
	}
	logStartup(cfg)

	db, err := sql.Open("postgres", cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("db: %v", err)
	}
	defer db.Close()

	st := store.New(db)
	provider := integrations.NewProviderClient(cfg.Provider, cfg.HTTPTimeout)
	h := handlers.New(cfg, st, auth.NewTokenSource(), provider)

	api := http.NewServeMux()
	api.HandleFunc("GET /vehicles", h.ListVehicles)
	api.HandleFunc("GET /vehicles/{id}", h.GetVehicle)
	api.HandleFunc("GET /vehicles/{id}/maintenance", auth.RequireVehicleAccess(st.VehicleFleet, h.GetVehicleMaintenance))
	api.HandleFunc("GET /vehicles/{id}/documents", h.DownloadVehicleDocument)
	api.HandleFunc("POST /vehicles/{id}/logs/archive", auth.RequireRole("admin", h.ArchiveVehicleLogs))
	api.HandleFunc("POST /vehicles/{id}/device-token", auth.RequireRole("admin", h.IssueDeviceToken))
	api.HandleFunc("POST /providers/{name}/sync", auth.RequireRole("admin", h.SyncProvider))
	api.HandleFunc("GET /drivers/{id}/trips", h.ListDriverTrips)
	api.HandleFunc("GET /trips/{id}", h.GetTrip)
	api.HandleFunc("POST /trips/{id}/close", h.CloseTrip)
	api.HandleFunc("GET /trips/{id}/export", h.ExportTrip)
	api.HandleFunc("POST /trips/{id}/share", h.CreateShareLink)
	api.HandleFunc("GET /trips/exports/{ref}", h.DownloadTripExport)
	api.HandleFunc("POST /trips/routes/import", h.ImportRoute)
	api.HandleFunc("GET /geofences", h.ListGeofences)
	api.HandleFunc("POST /geofences", h.CreateGeofence)
	api.HandleFunc("POST /geofences/webhook/test", h.TestGeofenceWebhook)
	api.HandleFunc("GET /reports/trips/{id}", h.TripReportPage)
	api.HandleFunc("GET /reports/geofences/{id}/banner", h.GeofenceAlertBanner)

	root := http.NewServeMux()
	root.Handle("/api/", http.StripPrefix("/api", auth.Authenticate(auth.NewSigner(cfg.SessionKey), api)))
	root.HandleFunc("POST /login", h.Login)
	root.Handle("GET /logout", auth.SanitizeNext(http.HandlerFunc(h.Logout)))
	root.HandleFunc("GET /dispatch", h.DispatchBoard)

	srv := &http.Server{
		Addr:              cfg.Addr,
		Handler:           root,
		ReadHeaderTimeout: 10 * time.Second,
	}
	log.Printf("fleettrack listening on %s", cfg.Addr)
	log.Fatal(srv.ListenAndServe())
}

func logStartup(cfg *config.Config) {
	log.Printf("env=%s addr=%s docs=%s exports=%s", cfg.Env, cfg.Addr, cfg.DocsDir, cfg.ExportDir)
	log.Printf("telematics provider: %v", cfg.Provider)
	log.Printf("provider feeds configured: %d", len(cfg.ProviderFeeds))
}
