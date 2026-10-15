package handlers

import (
	"errors"
	"io"
	"log"
	"net/http"
	"os"
	"regexp"
	"strings"
)

var statementIDPattern = regexp.MustCompile(`^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$`)

func (h *Handler) DownloadStatement(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	if !statementIDPattern.MatchString(id) {
		writeError(w, http.StatusBadRequest, "invalid statement id")
		return
	}
	f, err := h.archive.Open(id)
	if errors.Is(err, os.ErrNotExist) {
		writeError(w, http.StatusNotFound, "statement not found")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "open failed")
		return
	}
	defer f.Close()
	w.Header().Set("Content-Type", "text/csv")
	if _, err := io.Copy(w, f); err != nil {
		log.Printf("stream statement %s: %v", id, err)
	}
}

func (h *Handler) ArchiveStatement(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	if !statementIDPattern.MatchString(id) {
		writeError(w, http.StatusBadRequest, "invalid statement id")
		return
	}
	if err := h.archive.Compress(r.Context(), id); err != nil {
		log.Printf("archive statement %s: %v", id, err)
		writeError(w, http.StatusInternalServerError, "archive failed")
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) GetRate(w http.ResponseWriter, r *http.Request) {
	rate, ok := h.rates.Get(strings.ToUpper(r.PathValue("pair")))
	if !ok {
		writeError(w, http.StatusNotFound, "rate not cached")
		return
	}
	writeJSON(w, http.StatusOK, map[string]float64{"rate": rate})
}

func (h *Handler) RefreshRate(w http.ResponseWriter, r *http.Request) {
	pair := strings.ToUpper(r.PathValue("pair"))
	if len(pair) != 6 {
		writeError(w, http.StatusBadRequest, "pair must look like EURUSD")
		return
	}
	rate, err := h.fx.FetchRate(r.Context(), pair[:3], pair[3:])
	if err != nil {
		writeError(w, http.StatusBadGateway, "fx unavailable")
		return
	}
	h.rates.Put(pair, rate)
	writeJSON(w, http.StatusOK, map[string]float64{"rate": rate})
}
