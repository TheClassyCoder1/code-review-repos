package handlers

import (
	"log"
	"net/http"

	"fleettrack/internal/auth"
	"fleettrack/internal/integrations"
	"fleettrack/internal/store"
)

func (h *Handler) ListGeofences(w http.ResponseWriter, r *http.Request) {
	u := auth.UserFrom(r.Context())
	fences, err := h.db.ListGeofences(r.Context(), u.FleetID)
	if err != nil {
		storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, fences)
}

func (h *Handler) CreateGeofence(w http.ResponseWriter, r *http.Request) {
	u := auth.UserFrom(r.Context())
	var in geofenceInput
	if err := decodeJSON(w, r, &in); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	if err := validateGeofence(in); err != nil {
		writeError(w, http.StatusUnprocessableEntity, err.Error())
		return
	}
	id, err := h.db.InsertGeofence(r.Context(), store.Geofence{
		FleetID:   u.FleetID,
		Name:      in.Name,
		CenterLat: in.CenterLat,
		CenterLon: in.CenterLon,
		RadiusM:   in.RadiusM,
	})
	if err != nil {
		storeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, map[string]int64{"id": id})
}

func (h *Handler) TestGeofenceWebhook(w http.ResponseWriter, r *http.Request) {
	var body struct {
		URL string `json:"url"`
	}
	if err := decodeJSON(w, r, &body); err != nil || body.URL == "" {
		writeError(w, http.StatusBadRequest, "url is required")
		return
	}
	status, err := integrations.PingWebhook(r.Context(), h.client, body.URL, integrations.SigningSecret(h.cfg))
	if err != nil {
		log.Printf("webhook test failed: %v", err)
		writeError(w, http.StatusBadGateway, "webhook unreachable")
		return
	}
	writeJSON(w, http.StatusOK, map[string]int{"status": status})
}
