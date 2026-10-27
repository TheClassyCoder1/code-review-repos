package handlers

import (
	"encoding/csv"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"

	"meterhub/internal/auth"
	"meterhub/internal/billing"
	"meterhub/internal/ingest"
	"meterhub/internal/store"
)

func (h *Handler) UploadReadingsCSV(w http.ResponseWriter, r *http.Request) {
	data, err := io.ReadAll(r.Body)
	if err != nil {
		writeError(w, http.StatusBadRequest, "could not read upload")
		return
	}
	var rs []store.Reading
	for n, line := range strings.Split(string(data), "\n") {
		if strings.TrimSpace(line) == "" || (n == 0 && strings.HasPrefix(line, "meter_id")) {
			continue
		}
		rd, err := ingest.ParseReadingLine(line)
		if err != nil {
			writeError(w, http.StatusBadRequest, "line "+strconv.Itoa(n+1)+": "+err.Error())
			return
		}
		rs = append(rs, rd)
	}
	inserted, err := h.db.InsertReadings(r.Context(), rs)
	if err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]int64{"received": int64(len(rs)), "inserted": inserted})
}

func (h *Handler) GatewayWebhook(w http.ResponseWriter, r *http.Request) {
	body, err := io.ReadAll(http.MaxBytesReader(w, r.Body, h.cfg.MaxBodyBytes))
	if err != nil {
		writeError(w, http.StatusRequestEntityTooLarge, "payload too large")
		return
	}
	if !auth.VerifyGatewaySignature(h.cfg.GatewayToken, body, r.Header.Get("X-Gateway-Signature")) {
		writeError(w, http.StatusUnauthorized, "bad signature")
		return
	}
	var b ingest.Batch
	if err := json.Unmarshal(body, &b); err != nil {
		writeError(w, http.StatusBadRequest, "invalid batch")
		return
	}
	inserted, err := h.db.InsertReadings(r.Context(), b.Readings)
	if err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{"batch_id": b.ID, "inserted": inserted})
}

func (h *Handler) Snapshot(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	rs, err := h.db.LatestReadings(r.Context(), id, 96)
	if err != nil {
		h.storeError(w, err)
		return
	}
	if len(rs) == 0 {
		writeError(w, http.StatusNotFound, "no readings for meter")
		return
	}
	writeJSON(w, http.StatusOK, billing.MeterSnapshot(rs))
}

func (h *Handler) ExportReadingsCSV(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	from, err1 := time.Parse("2006-01-02", r.URL.Query().Get("from"))
	to, err2 := time.Parse("2006-01-02", r.URL.Query().Get("to"))
	if err1 != nil || err2 != nil || !to.After(from) {
		writeError(w, http.StatusBadRequest, "from and to (YYYY-MM-DD) are required")
		return
	}
	rows, err := h.db.StreamReadings(r.Context(), id, from, to)
	if err != nil {
		h.storeError(w, err)
		return
	}
	defer rows.Close()
	w.Header().Set("Content-Type", "text/csv")
	cw := csv.NewWriter(w)
	cw.Write([]string{"meter_id", "read_at", "watt_hours"})
	for rows.Next() {
		var rd store.Reading
		if err := rows.Scan(&rd.MeterID, &rd.ReadAt, &rd.WattHours); err != nil {
			log.Printf("export meter=%d: scan: %v", id, err)
			break
		}
		cw.Write([]string{strconv.FormatInt(rd.MeterID, 10), rd.ReadAt.Format(time.RFC3339), strconv.FormatInt(rd.WattHours, 10)})
	}
	cw.Flush()
}

func parseMonth(s string) (time.Time, time.Time, bool) {
	start, err := time.Parse("2006-01", s)
	if err != nil {
		return time.Time{}, time.Time{}, false
	}
	return start, start.AddDate(0, 1, 0), true
}

func (h *Handler) TriggerRecalc(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	if _, err := h.db.GetMeter(r.Context(), id); err != nil {
		h.storeError(w, err)
		return
	}
	go h.recalc.Recalculate(id)
	writeJSON(w, http.StatusAccepted, map[string]any{"meter_id": id, "status": "recalculating"})
}
