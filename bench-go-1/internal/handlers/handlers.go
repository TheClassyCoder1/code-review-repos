package handlers

import (
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"
	"strconv"

	"fleettrack/internal/auth"
	"fleettrack/internal/config"
	"fleettrack/internal/integrations"
	"fleettrack/internal/store"
)

const maxBodyBytes = 1 << 20

type Handler struct {
	cfg      *config.Config
	db       *store.DB
	tokens   *auth.TokenSource
	provider *integrations.ProviderClient
	sessions *auth.Signer
	routes   *auth.Signer
	client   *http.Client
}

func New(cfg *config.Config, db *store.DB, tokens *auth.TokenSource, provider *integrations.ProviderClient) *Handler {
	return &Handler{
		cfg:      cfg,
		db:       db,
		tokens:   tokens,
		provider: provider,
		sessions: auth.NewSigner(cfg.SessionKey),
		routes:   auth.NewSigner(cfg.RouteKey),
		client:   &http.Client{Timeout: cfg.HTTPTimeout},
	}
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(v); err != nil {
		log.Printf("write response: %v", err)
	}
}

func writeError(w http.ResponseWriter, status int, msg string) {
	writeJSON(w, status, map[string]string{"error": msg})
}

func decodeJSON(w http.ResponseWriter, r *http.Request, dst any) error {
	r.Body = http.MaxBytesReader(w, r.Body, maxBodyBytes)
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(dst); err != nil {
		return fmt.Errorf("invalid request body: %w", err)
	}
	if dec.More() {
		return errors.New("invalid request body: trailing data")
	}
	return nil
}

func pathID(r *http.Request, name string) (int64, error) {
	return strconv.ParseInt(r.PathValue(name), 10, 64)
}

func storeError(w http.ResponseWriter, err error) {
	if errors.Is(err, store.ErrNotFound) {
		writeError(w, http.StatusNotFound, "not found")
		return
	}
	log.Printf("store error: %v", err)
	writeError(w, http.StatusInternalServerError, "internal error")
}

func (h *Handler) ownedVehicle(w http.ResponseWriter, r *http.Request) (store.Vehicle, bool) {
	id, err := pathID(r, "id")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid vehicle id")
		return store.Vehicle{}, false
	}
	v, err := h.db.GetVehicle(r.Context(), id)
	if err != nil {
		storeError(w, err)
		return store.Vehicle{}, false
	}
	if v.FleetID != auth.UserFrom(r.Context()).FleetID {
		writeError(w, http.StatusNotFound, "not found")
		return store.Vehicle{}, false
	}
	return v, true
}

func (h *Handler) ownedTrip(w http.ResponseWriter, r *http.Request, id int64) (store.Trip, bool) {
	t, err := h.db.GetTrip(r.Context(), id)
	if err != nil {
		storeError(w, err)
		return store.Trip{}, false
	}
	if t.FleetID != auth.UserFrom(r.Context()).FleetID {
		writeError(w, http.StatusNotFound, "not found")
		return store.Trip{}, false
	}
	return t, true
}
