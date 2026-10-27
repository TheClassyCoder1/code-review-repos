package handlers

import (
	"encoding/base64"
	"io"
	"log"
	"net/http"
	"regexp"
	"strconv"
	"strings"
	"time"

	"fleettrack/internal/auth"
	"fleettrack/internal/export"
	"fleettrack/internal/geo"
)

const shareLinkTTL = 72 * time.Hour

var tripRefPattern = regexp.MustCompile(`^trip-[0-9]{1,12}$`)

func (h *Handler) GetTrip(w http.ResponseWriter, r *http.Request) {
	id, err := pathID(r, "id")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid trip id")
		return
	}
	t, err := h.db.GetTrip(r.Context(), id)
	if err != nil {
		storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, t)
}

func (h *Handler) ListDriverTrips(w http.ResponseWriter, r *http.Request) {
	u := auth.UserFrom(r.Context())
	driverID, err := pathID(r, "id")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid driver id")
		return
	}
	trips, err := h.db.ListTripsForDriver(r.Context(), u.FleetID, driverID, r.URL.Query().Get("status"))
	if err != nil {
		storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, trips)
}

func (h *Handler) CloseTrip(w http.ResponseWriter, r *http.Request) {
	id, err := pathID(r, "id")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid trip id")
		return
	}
	if _, ok := h.ownedTrip(w, r, id); !ok {
		return
	}
	var body struct {
		DistanceKm float64 `json:"distance_km"`
	}
	if err := decodeJSON(w, r, &body); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	if body.DistanceKm < 0 || body.DistanceKm > 5000 {
		writeError(w, http.StatusUnprocessableEntity, "distance_km out of range")
		return
	}
	if err := h.db.CloseTrip(r.Context(), id, time.Now().UTC(), body.DistanceKm); err != nil {
		storeError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) ExportTrip(w http.ResponseWriter, r *http.Request) {
	id, err := pathID(r, "id")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid trip id")
		return
	}
	t, ok := h.ownedTrip(w, r, id)
	if !ok {
		return
	}
	format := r.URL.Query().Get("format")
	if format == "" {
		format = export.DefaultFormat
	}
	data, err := export.ConvertTrip(r.Context(), export.TripGPXPath(h.cfg.ExportDir, t.ID), format)
	if err != nil {
		log.Printf("export trip %d: %v", t.ID, err)
		writeError(w, http.StatusInternalServerError, "export failed")
		return
	}
	w.Header().Set("Content-Type", export.ExportContentType("trip."+format))
	w.Write(data)
}

func (h *Handler) DownloadTripExport(w http.ResponseWriter, r *http.Request) {
	ref := r.PathValue("ref")
	if !tripRefPattern.MatchString(ref) {
		writeError(w, http.StatusBadRequest, "invalid export reference")
		return
	}
	id, _ := strconv.ParseInt(strings.TrimPrefix(ref, "trip-"), 10, 64)
	if _, ok := h.ownedTrip(w, r, id); !ok {
		return
	}
	f, err := export.OpenTripExport(h.cfg.ExportDir, ref)
	if err != nil {
		writeError(w, http.StatusNotFound, "export not found")
		return
	}
	defer f.Close()
	w.Header().Set("Content-Type", export.ExportContentType(f.Name()))
	if _, err := io.Copy(w, f); err != nil {
		log.Printf("stream export %s: %v", ref, err)
	}
}

func (h *Handler) ImportRoute(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Route     string `json:"route"`
		Signature string `json:"signature"`
	}
	if err := decodeJSON(w, r, &body); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	blob, err := base64.StdEncoding.DecodeString(body.Route)
	if err != nil {
		writeError(w, http.StatusBadRequest, "route must be base64")
		return
	}
	if !h.routes.ValidMAC(blob, body.Signature) {
		writeError(w, http.StatusUnauthorized, "route signature mismatch")
		return
	}
	route, err := geo.DecodeRoute(blob)
	if err != nil {
		writeError(w, http.StatusBadRequest, "malformed route")
		return
	}
	if _, ok := h.ownedTrip(w, r, route.TripID); !ok {
		return
	}
	writeJSON(w, http.StatusOK, geo.SummarizeRoute(route.Points))
}

func (h *Handler) CreateShareLink(w http.ResponseWriter, r *http.Request) {
	id, err := pathID(r, "id")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid trip id")
		return
	}
	t, ok := h.ownedTrip(w, r, id)
	if !ok {
		return
	}
	link := auth.SignShareLink(auth.NewShareToken(t.ID), time.Now().Add(shareLinkTTL).UTC())
	writeJSON(w, http.StatusCreated, link)
}
