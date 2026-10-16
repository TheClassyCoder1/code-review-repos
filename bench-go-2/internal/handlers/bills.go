package handlers

import (
	"log"
	"net/http"
	"strconv"
	"time"

	"meterhub/internal/billing"
	"meterhub/internal/store"
)

func (h *Handler) CloseBillingPeriod(w http.ResponseWriter, r *http.Request) {
	var req struct {
		PeriodEnd string `json:"period_end"`
	}
	if !h.decodeJSON(w, r, &req) {
		return
	}
	end, err := time.Parse("2006-01-02", req.PeriodEnd)
	if err != nil {
		writeError(w, http.StatusBadRequest, "period_end must be YYYY-MM-DD")
		return
	}
	issued, err := h.db.MarkBillsIssued(r.Context(), end)
	if err != nil {
		log.Printf("close period %s: %v", req.PeriodEnd, err)
	}
	writeJSON(w, http.StatusOK, map[string]any{"period_end": req.PeriodEnd, "status": "closed", "issued": issued})
}

func (h *Handler) IssueBill(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	var req struct {
		Month        string  `json:"month"`
		ExportedWh   int64   `json:"exported_wh"`
		FeedInPerKWh float64 `json:"feed_in_per_kwh"`
	}
	if !h.decodeJSON(w, r, &req) {
		return
	}
	from, to, valid := parseMonth(req.Month)
	if !valid || req.ExportedWh < 0 || req.FeedInPerKWh < 0 {
		writeError(w, http.StatusBadRequest, "month must be YYYY-MM")
		return
	}
	m, err := h.db.GetMeter(r.Context(), id)
	if err != nil {
		h.storeError(w, err)
		return
	}
	bill, lines, err := h.recalc.BuildBill(r.Context(), m, from, to)
	if err != nil {
		h.storeError(w, err)
		return
	}
	if credit := billing.SolarExportCredit(req.ExportedWh, req.FeedInPerKWh); credit > 0 {
		lines = append(lines, store.BillLine{Description: "Solar export credit", AmountCents: -credit})
		bill.AmountCents -= credit
	}
	if err := h.db.IssueBill(r.Context(), bill, lines); err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, map[string]any{"bill": bill, "lines": lines})
}

func (h *Handler) ListBills(w http.ResponseWriter, r *http.Request) {
	accountID, ok := pathID(w, r, "account")
	if !ok {
		return
	}
	limit := 24
	if v := r.URL.Query().Get("limit"); v != "" {
		n, err := strconv.Atoi(v)
		if err != nil || n < 1 || n > 120 {
			writeError(w, http.StatusBadRequest, "limit must be between 1 and 120")
			return
		}
		limit = n
	}
	bills, err := h.db.ListBillsForAccount(r.Context(), accountID, limit)
	if err != nil {
		h.storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, bills)
}

func (h *Handler) GetInvoice(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	bill, lines, err := h.db.GetBillWithCustomer(r.Context(), id)
	if err != nil {
		h.storeError(w, err)
		return
	}
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	w.Write([]byte(billing.RenderInvoiceText(bill, lines)))
}
