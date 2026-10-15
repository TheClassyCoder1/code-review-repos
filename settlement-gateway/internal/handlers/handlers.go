package handlers

import (
	"crypto/rand"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"sync"

	"github.com/theclassycoder1/code-review-repos/settlement-gateway/internal/client"
	"github.com/theclassycoder1/code-review-repos/settlement-gateway/internal/export"
	"github.com/theclassycoder1/code-review-repos/settlement-gateway/internal/store"
)

type Handler struct {
	settlements *store.SettlementStore
	rates       *store.RateCache
	totalsMu    sync.Mutex
	totals      *store.Totals
	bank        *client.BankClient
	fx          *client.FXClient
	archive     *export.Archiver
}

func New(settlements *store.SettlementStore, rates *store.RateCache, totals *store.Totals,
	bank *client.BankClient, fx *client.FXClient, archive *export.Archiver) *Handler {
	return &Handler{
		settlements: settlements,
		rates:       rates,
		totals:      totals,
		bank:        bank,
		fx:          fx,
		archive:     archive,
	}
}

func (h *Handler) Health(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
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

func newID() string {
	b := make([]byte, 16)
	if _, err := rand.Read(b); err != nil {
		panic(err)
	}
	b[6] = (b[6] & 0x0f) | 0x40
	b[8] = (b[8] & 0x3f) | 0x80
	return fmt.Sprintf("%x-%x-%x-%x-%x", b[0:4], b[4:6], b[6:8], b[8:10], b[10:16])
}
