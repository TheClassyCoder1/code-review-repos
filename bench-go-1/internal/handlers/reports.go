package handlers

import (
	"fmt"
	"html"
	"html/template"
	"log"
	"net/http"

	"fleettrack/internal/auth"
)

var bannerTmpl = template.Must(template.New("banner").Parse(
	`<div class="geofence-alert">{{.Label}} &middot; radius {{printf "%.0f" .RadiusM}} m</div>`))

func (h *Handler) TripReportPage(w http.ResponseWriter, r *http.Request) {
	id, err := pathID(r, "id")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid trip id")
		return
	}
	t, ok := h.ownedTrip(w, r, id)
	if !ok {
		return
	}
	title := r.URL.Query().Get("title")
	if title == "" {
		title = fmt.Sprintf("Trip %d", t.ID)
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	fmt.Fprintf(w, "<html><head><title>%s</title></head><body><h1>%s</h1>", title, title)
	fmt.Fprintf(w, "<p>Status: %s</p><p>Distance: %.1f km</p></body></html>", html.EscapeString(t.Status), t.DistanceKm)
}

type bannerView struct {
	Label   template.HTML
	RadiusM float64
}

func (h *Handler) GeofenceAlertBanner(w http.ResponseWriter, r *http.Request) {
	id, err := pathID(r, "id")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid geofence id")
		return
	}
	g, err := h.db.GetGeofence(r.Context(), id)
	if err != nil {
		storeError(w, err)
		return
	}
	if g.FleetID != auth.UserFrom(r.Context()).FleetID {
		writeError(w, http.StatusNotFound, "not found")
		return
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	view := bannerView{Label: template.HTML("<strong>" + g.Name + "</strong>"), RadiusM: g.RadiusM}
	if err := bannerTmpl.Execute(w, view); err != nil {
		log.Printf("render geofence banner %d: %v", g.ID, err)
	}
}
