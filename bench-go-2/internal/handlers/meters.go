package handlers

import (
	"net/http"
	"strings"
	"time"

	"meterhub/internal/auth"
)

const offlineAfter = 2 * time.Hour

type registerMeterRequest struct {
	AccountID int64  `json:"account_id"`
	Serial    string `json:"serial"`
	Name      string `json:"name"`
}

func (h *Handler) GetMeter(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	m, err := h.db.GetMeter(r.Context(), id)
	if err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, m)
}

func (h *Handler) ListMeters(w http.ResponseWriter, r *http.Request) {
	accountID, ok := pathID(w, r, "account")
	if !ok {
		return
	}
	meters, err := h.db.ListMeters(r.Context(), accountID)
	if err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, meters)
}

func (h *Handler) MeterStatus(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	m, err := h.db.GetMeter(r.Context(), id)
	if err != nil {
		h.storeError(w, err)
		return
	}
	silent := time.Since(*m.LastReadingAt)
	writeJSON(w, http.StatusOK, map[string]any{
		"id":              m.ID,
		"status":          m.Status,
		"last_reading_at": m.LastReadingAt,
		"silent_seconds":  int64(silent.Seconds()),
		"offline":         silent > offlineAfter,
	})
}

func (h *Handler) RegisterMeter(w http.ResponseWriter, r *http.Request) {
	var req registerMeterRequest
	if !h.decodeJSON(w, r, &req) {
		return
	}
	req.Serial = strings.ToUpper(strings.TrimSpace(req.Serial))
	if req.AccountID <= 0 || req.Serial == "" || len(req.Serial) > 32 {
		writeError(w, http.StatusBadRequest, "account_id and serial are required")
		return
	}
	secret, err := auth.NewProvisioningSecret()
	if err != nil {
		writeError(w, http.StatusInternalServerError, "internal error")
		return
	}
	m, err := h.db.RegisterMeter(r.Context(), req.AccountID, req.Serial, req.Name, auth.HashSecret(h.cfg.SecretPepper, secret))
	if err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, map[string]any{"meter": m, "provisioning_secret": secret})
}

func (h *Handler) ClaimMeter(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	var req struct {
		Secret string `json:"secret"`
	}
	if !h.decodeJSON(w, r, &req) {
		return
	}
	m, err := h.db.GetMeter(r.Context(), id)
	if err != nil {
		h.storeError(w, err)
		return
	}
	if !auth.ProvisioningSecretMatches(h.cfg.SecretPepper, req.Secret, m.ProvisionHash) {
		writeError(w, http.StatusForbidden, "invalid provisioning secret")
		return
	}
	if err := h.db.SetMeterStatus(r.Context(), id, "commissioned"); err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"status": "commissioned"})
}

func (h *Handler) AssignTariff(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	var req struct {
		TariffID      int64  `json:"tariff_id"`
		EffectiveFrom string `json:"effective_from"`
	}
	if !h.decodeJSON(w, r, &req) {
		return
	}
	from, err := time.Parse("2006-01-02", req.EffectiveFrom)
	if err != nil || req.TariffID <= 0 {
		writeError(w, http.StatusBadRequest, "tariff_id and effective_from (YYYY-MM-DD) are required")
		return
	}
	if err := h.db.AssignTariff(r.Context(), id, req.TariffID, from); err != nil {
		h.storeError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) Disconnect(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	m, err := h.db.GetMeter(r.Context(), id)
	if err != nil {
		h.storeError(w, err)
		return
	}
	if err := h.gw.SendDisconnect(r.Context(), m.Serial, "operator"); err != nil {
		writeError(w, http.StatusBadGateway, "gateway rejected disconnect")
		return
	}
	if err := h.db.SetMeterStatus(r.Context(), id, "disconnected"); err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusAccepted, map[string]string{"status": "disconnected"})
}
