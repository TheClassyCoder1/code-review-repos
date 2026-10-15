package handlers

import (
	"encoding/json"
	"errors"
	"net/http"
	"time"

	"github.com/theclassycoder1/code-review-repos/settlement-gateway/internal/client"
	"github.com/theclassycoder1/code-review-repos/settlement-gateway/internal/store"
	"github.com/theclassycoder1/code-review-repos/settlement-gateway/internal/token"
)

var sortColumns = map[string]string{
	"created": "created_at",
	"amount":  "amount_minor",
	"status":  "status",
}

func (h *Handler) ListSettlements(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	if merchant := q.Get("merchant_id"); merchant != "" {
		items, err := h.settlements.ListByMerchant(r.Context(), merchant, q.Get("status"))
		if err != nil {
			writeError(w, http.StatusInternalServerError, "list failed")
			return
		}
		writeJSON(w, http.StatusOK, items)
		return
	}
	column, ok := sortColumns[q.Get("sort")]
	if !ok {
		column = sortColumns["created"]
	}
	items, err := h.settlements.ListSorted(r.Context(), column, 100)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "list failed")
		return
	}
	writeJSON(w, http.StatusOK, items)
}

func (h *Handler) GetSettlement(w http.ResponseWriter, r *http.Request) {
	st, err := h.settlements.Get(r.Context(), r.PathValue("id"))
	if errors.Is(err, store.ErrNotFound) {
		writeError(w, http.StatusNotFound, "settlement not found")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "lookup failed")
		return
	}
	writeJSON(w, http.StatusOK, st)
}

type createSettlementRequest struct {
	MerchantID  string `json:"merchant_id"`
	AmountMinor int64  `json:"amount_minor"`
	Currency    string `json:"currency"`
	ReturnURL   string `json:"return_url"`
}

func (h *Handler) CreateSettlement(w http.ResponseWriter, r *http.Request) {
	var req createSettlementRequest
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<16)).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid body")
		return
	}
	if req.MerchantID == "" || req.AmountMinor <= 0 || len(req.Currency) != 3 {
		writeError(w, http.StatusBadRequest, "merchant_id, amount_minor and currency are required")
		return
	}
	st := &store.Settlement{
		ID:          newID(),
		MerchantID:  req.MerchantID,
		AmountMinor: req.AmountMinor,
		Currency:    req.Currency,
		Status:      "pending",
		ClaimToken:  token.NewPayoutClaimToken(),
		CreatedAt:   time.Now().UTC(),
	}
	if err := h.settlements.Insert(r.Context(), st); err != nil {
		writeError(w, http.StatusInternalServerError, "create failed")
		return
	}
	resp := map[string]string{"id": st.ID, "claim_token": st.ClaimToken}
	if req.ReturnURL != "" {
		resp["return_url"] = h.bank.ReturnURL(req.ReturnURL, st.ID)
	}
	writeJSON(w, http.StatusCreated, resp)
}

func (h *Handler) ApproveSettlement(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	st, err := h.settlements.Get(ctx, r.PathValue("id"))
	if errors.Is(err, store.ErrNotFound) {
		writeError(w, http.StatusNotFound, "settlement not found")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "lookup failed")
		return
	}
	if st.Status != "pending" {
		writeError(w, http.StatusConflict, "settlement is not pending")
		return
	}
	payoutID, err := h.bank.SubmitPayout(ctx, client.Payout{SettlementID: st.ID, AmountMinor: st.AmountMinor, Currency: st.Currency})
	if err != nil {
		writeError(w, http.StatusBadGateway, "bank rejected payout")
		return
	}
	if err := h.settlements.UpdateStatus(ctx, st.ID, "approved", payoutID); err != nil {
		writeError(w, http.StatusInternalServerError, "update failed")
		return
	}
	h.totalsMu.Lock()
	h.totals.Add(st.Currency, st.AmountMinor)
	h.totalsMu.Unlock()
	writeJSON(w, http.StatusOK, map[string]string{"payout_id": payoutID})
}

func (h *Handler) GetTotals(w http.ResponseWriter, r *http.Request) {
	h.totalsMu.Lock()
	snapshot := h.totals.Snapshot()
	h.totalsMu.Unlock()
	writeJSON(w, http.StatusOK, snapshot)
}

func (h *Handler) GetPayoutStatus(w http.ResponseWriter, r *http.Request) {
	status, err := h.bank.PayoutStatus(r.Context(), r.PathValue("id"))
	if errors.Is(err, client.ErrUnknownPayout) {
		writeError(w, http.StatusNotFound, "payout not found")
		return
	}
	if err != nil {
		writeError(w, http.StatusBadGateway, "bank unavailable")
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"status": status})
}
