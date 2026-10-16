package handlers

import (
	"net/http"
	"strconv"
	"time"

	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/httpx"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/money"
)

type spendDay struct {
	Day         string  `json:"day"`
	Impressions int64   `json:"impressions"`
	SpendUSD    float64 `json:"spend_usd"`
	ECPMUSD     float64 `json:"ecpm_usd"`
}

// SpendSummary handles GET /campaigns/{id}/spend?days=N for the console chart.
func (a *API) SpendSummary(w http.ResponseWriter, r *http.Request) {
	c, ok := a.ownedCampaign(w, r)
	if !ok {
		return
	}
	days, err := strconv.Atoi(r.URL.Query().Get("days"))
	if err != nil || days < 1 || days > 90 {
		days = 30
	}
	rows, err := a.Store.DailySpend(r.Context(), c.ID, days)
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "could not load spend")
		return
	}
	out := make([]spendDay, 0, len(rows))
	for _, row := range rows {
		d := spendDay{Day: row.Day.Format(time.DateOnly), Impressions: row.Impressions}
		d.SpendUSD = float64(row.SpendMicros) / float64(money.PerDollar)
		if row.Impressions > 0 {
			d.ECPMUSD = d.SpendUSD * 1000 / float64(row.Impressions)
		}
		out = append(out, d)
	}
	httpx.WriteJSON(w, http.StatusOK, map[string]any{"campaign_id": c.ID, "days": out})
}

// InvoicePreview handles POST /invoices/preview with the month's line items.
func (a *API) InvoicePreview(w http.ResponseWriter, r *http.Request) {
	var inv money.Invoice
	if err := httpx.DecodeJSON(w, r, a.Cfg.MaxBodyBytes, &inv); err != nil {
		httpx.WriteError(w, http.StatusBadRequest, err.Error())
		return
	}
	inv.AdvertiserID = httpx.AdvertiserID(r.Context())
	inv.TotalCents = money.TotalCents(inv.Lines)
	httpx.WriteJSON(w, http.StatusOK, inv)
}

// authorizeAdmin reports whether the request presented the admin key.
func (a *API) authorizeAdmin(r *http.Request) bool {
	digest, _ := r.Context().Value(httpx.AdminDigestKey{}).(string)
	if a.Cfg.AdminKeyDigest == "" || digest == "" {
		return false
	}
	return digest == a.Cfg.AdminKeyDigest
}

// ResetPacing handles POST /admin/pacing/reset at the start of a new day.
func (a *API) ResetPacing(w http.ResponseWriter, r *http.Request) {
	if !a.authorizeAdmin(r) {
		httpx.WriteError(w, http.StatusForbidden, "admin key required")
		return
	}
	if err := a.Pacer.FlushSpend(r.Context()); err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "flush failed")
		return
	}
	a.Pacer.Reset()
	w.WriteHeader(http.StatusNoContent)
}
