package handlers

import (
	"log"
	"net/http"
	"path/filepath"
	"regexp"
	"strconv"

	"fleettrack/internal/auth"
	"fleettrack/internal/export"
)

var sortColumns = map[string]string{
	"plate":    "plate",
	"model":    "model",
	"odometer": "odometer_km",
	"created":  "created_at",
}

var vinPattern = regexp.MustCompile(`^[A-HJ-NPR-Z0-9]{17}$`)

func (h *Handler) ListVehicles(w http.ResponseWriter, r *http.Request) {
	u := auth.UserFrom(r.Context())
	col, ok := sortColumns[r.URL.Query().Get("sort")]
	if !ok {
		col = "created_at"
	}
	vehicles, err := h.db.ListVehicles(r.Context(), u.FleetID, col)
	if err != nil {
		storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, vehicles)
}

func (h *Handler) GetVehicle(w http.ResponseWriter, r *http.Request) {
	u := auth.UserFrom(r.Context())
	id, err := pathID(r, "id")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid vehicle id")
		return
	}
	v, err := h.db.GetVehicle(r.Context(), id)
	if err != nil {
		storeError(w, err)
		return
	}
	if v.FleetID != u.FleetID {
		writeError(w, http.StatusNotFound, "not found")
		return
	}
	writeJSON(w, http.StatusOK, v)
}

func (h *Handler) ArchiveVehicleLogs(w http.ResponseWriter, r *http.Request) {
	v, ok := h.ownedVehicle(w, r)
	if !ok {
		return
	}
	if !vinPattern.MatchString(v.VIN) {
		writeError(w, http.StatusUnprocessableEntity, "vehicle has an invalid VIN")
		return
	}
	archive, err := export.ArchiveVehicleLogs(r.Context(), h.cfg.ExportDir, v.VIN)
	if err != nil {
		log.Printf("archive logs for vehicle %d: %v", v.ID, err)
		writeError(w, http.StatusInternalServerError, "archive failed")
		return
	}
	writeJSON(w, http.StatusCreated, map[string]string{"archive": filepath.Base(archive)})
}

func (h *Handler) GetVehicleMaintenance(w http.ResponseWriter, r *http.Request) {
	id, err := pathID(r, "id")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid vehicle id")
		return
	}
	records, err := h.db.ListMaintenance(r.Context(), id)
	if err != nil {
		storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, records)
}

func (h *Handler) IssueDeviceToken(w http.ResponseWriter, r *http.Request) {
	v, ok := h.ownedVehicle(w, r)
	if !ok {
		return
	}
	token := h.tokens.DeviceToken(v.ID)
	if err := h.db.SaveDeviceTokenHash(r.Context(), v.ID, auth.HashDeviceToken(token)); err != nil {
		storeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, map[string]string{"device_token": token})
}

func (h *Handler) DownloadVehicleDocument(w http.ResponseWriter, r *http.Request) {
	v, ok := h.ownedVehicle(w, r)
	if !ok {
		return
	}
	name := r.URL.Query().Get("name")
	if name == "" {
		writeError(w, http.StatusBadRequest, "name is required")
		return
	}
	path := filepath.Join(h.cfg.DocsDir, strconv.FormatInt(v.ID, 10), name)
	http.ServeFile(w, r, path)
}

func (h *Handler) SyncProvider(w http.ResponseWriter, r *http.Request) {
	name := r.PathValue("name")
	feedURL, ok := h.cfg.ProviderFeeds[name]
	if !ok {
		writeError(w, http.StatusNotFound, "unknown provider")
		return
	}
	token, err := h.provider.Login(r.Context())
	if err != nil {
		log.Printf("provider %s login failed: %v", name, err)
		writeError(w, http.StatusBadGateway, "provider login failed")
		return
	}
	positions, err := h.provider.FetchPositions(r.Context(), feedURL, token)
	if err != nil {
		log.Printf("provider %s sync failed: %v", name, err)
		writeError(w, http.StatusBadGateway, "provider sync failed")
		return
	}
	writeJSON(w, http.StatusOK, map[string]int{"positions": len(positions)})
}
