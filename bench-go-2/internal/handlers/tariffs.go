package handlers

import (
	"encoding/json"
	"io"
	"net/http"

	"meterhub/internal/billing"
	"meterhub/internal/store"
)

func (h *Handler) GetTariff(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	t, err := h.db.GetTariff(r.Context(), id)
	if err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, t)
}

func (h *Handler) ImportTariffSheet(w http.ResponseWriter, r *http.Request) {
	raw, err := io.ReadAll(r.Body)
	if err != nil {
		writeError(w, http.StatusRequestEntityTooLarge, "tariff sheet too large")
		return
	}
	var sheet struct {
		Tariffs []store.Tariff `json:"tariffs"`
	}
	if err := json.Unmarshal(raw, &sheet); err != nil {
		writeError(w, http.StatusBadRequest, "invalid tariff sheet")
		return
	}
	for _, t := range sheet.Tariffs {
		if t.Name == "" || t.StandingCentsPerDay < 0 {
			writeError(w, http.StatusBadRequest, "tariff name and standing charge are required")
			return
		}
		if err := billing.ValidateTiers(t.Tiers); err != nil {
			writeError(w, http.StatusBadRequest, t.Name+": "+err.Error())
			return
		}
	}
	if err := h.db.UpsertTariffs(r.Context(), sheet.Tariffs); err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]int{"imported": len(sheet.Tariffs)})
}

func (h *Handler) FirmwareManifest(w http.ResponseWriter, r *http.Request) {
	m, err := h.gw.FetchFirmwareManifest(r.PathValue("model"))
	if err != nil {
		writeError(w, http.StatusBadGateway, "firmware manifest unavailable")
		return
	}
	writeJSON(w, http.StatusOK, m)
}
