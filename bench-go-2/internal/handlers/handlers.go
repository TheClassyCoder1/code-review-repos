package handlers

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net/http"
	"strconv"

	"meterhub/internal/billing"
	"meterhub/internal/cache"
	"meterhub/internal/config"
	"meterhub/internal/ingest"
	"meterhub/internal/store"
)

type Handler struct {
	cfg    *config.Config
	db     *store.DB
	gw     *ingest.GatewayClient
	ingest *ingest.Service
	recalc *billing.Recalculator
	locks  *cache.KeyedMutex
}

func New(cfg *config.Config, db *store.DB, gw *ingest.GatewayClient, svc *ingest.Service, rc *billing.Recalculator) *Handler {
	return &Handler{cfg: cfg, db: db, gw: gw, ingest: svc, recalc: rc, locks: cache.NewKeyedMutex()}
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

func (h *Handler) storeError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, store.ErrNotFound):
		writeError(w, http.StatusNotFound, "not found")
	case errors.Is(err, store.ErrDuplicateSerial), errors.Is(err, store.ErrTariffAlreadyAssigned):
		writeError(w, http.StatusConflict, err.Error())
	default:
		log.Printf("store error: %v", err)
		writeError(w, http.StatusInternalServerError, "internal error")
	}
}

func pathID(w http.ResponseWriter, r *http.Request, name string) (int64, bool) {
	id, err := strconv.ParseInt(r.PathValue(name), 10, 64)
	if err != nil || id <= 0 {
		writeError(w, http.StatusBadRequest, "invalid "+name)
		return 0, false
	}
	return id, true
}

func (h *Handler) decodeJSON(w http.ResponseWriter, r *http.Request, dst any) bool {
	r.Body = http.MaxBytesReader(w, r.Body, h.cfg.MaxBodyBytes)
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(dst); err != nil {
		writeError(w, http.StatusBadRequest, fmt.Sprintf("invalid body: %v", err))
		return false
	}
	if _, err := dec.Token(); err != io.EOF {
		writeError(w, http.StatusBadRequest, "invalid body: trailing data")
		return false
	}
	return true
}

func (h *Handler) Health(w http.ResponseWriter, r *http.Request) {
	if err := h.db.PingContext(r.Context()); err != nil {
		writeError(w, http.StatusServiceUnavailable, "database unavailable")
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{"status": "ok", "ingest": h.ingest.Stats()})
}
