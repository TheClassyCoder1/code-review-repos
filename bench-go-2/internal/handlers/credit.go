package handlers

import (
	"database/sql"
	"net/http"
	"strings"

	"meterhub/internal/store"
)

type topUpRequest struct {
	AmountCents int64  `json:"amount_cents"`
	Reference   string `json:"reference"`
}

func (h *Handler) TopUp(w http.ResponseWriter, r *http.Request) {
	accountID, ok := pathID(w, r, "account")
	if !ok {
		return
	}
	var req topUpRequest
	if !h.decodeJSON(w, r, &req) {
		return
	}
	req.Reference = strings.TrimSpace(req.Reference)
	if req.AmountCents <= 0 || req.AmountCents > 100000 || req.Reference == "" {
		writeError(w, http.StatusBadRequest, "amount_cents (1..100000) and reference are required")
		return
	}
	var topupID int64
	err := h.db.WithTx(r.Context(), func(tx *sql.Tx) error {
		var err error
		topupID, err = store.RecordTopUp(r.Context(), tx, accountID, req.AmountCents, req.Reference)
		return err
	})
	if err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, map[string]int64{"topup_id": topupID})
}

func (h *Handler) GetBalance(w http.ResponseWriter, r *http.Request) {
	accountID, ok := pathID(w, r, "account")
	if !ok {
		return
	}
	a, err := h.db.GetAccount(r.Context(), accountID)
	if err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]int64{"account_id": a.ID, "balance_cents": a.BalanceCents})
}

func (h *Handler) AdjustBill(w http.ResponseWriter, r *http.Request) {
	billID, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	var req struct {
		DeltaCents int64  `json:"delta_cents"`
		Reason     string `json:"reason"`
	}
	if !h.decodeJSON(w, r, &req) {
		return
	}
	if req.DeltaCents == 0 || strings.TrimSpace(req.Reason) == "" {
		writeError(w, http.StatusBadRequest, "delta_cents and reason are required")
		return
	}
	unlock := h.locks.Lock(billID)
	defer unlock()
	amount, err := store.AdjustBillAmount(r.Context(), h.db, billID, req.DeltaCents)
	if err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]int64{"bill_id": billID, "amount_cents": amount})
}
