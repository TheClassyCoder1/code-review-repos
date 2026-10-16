package handlers

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net/http"
	"strings"
	"time"

	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/bidding"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/exchange"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/httpx"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/store"
)

// HandleBidRequest handles POST /bid from the ad server: it runs an auction
// and answers with the winning creative, or 204 when nothing may serve.
func (a *API) HandleBidRequest(w http.ResponseWriter, r *http.Request) {
	raw, err := io.ReadAll(r.Body)
	if err != nil {
		httpx.WriteError(w, http.StatusRequestEntityTooLarge, "bid request too large")
		return
	}
	var br exchange.BidRequest
	if err := json.Unmarshal(raw, &br); err != nil || br.RequestID == "" || br.Slot == "" {
		httpx.WriteError(w, http.StatusBadRequest, "request_id and slot are required")
		return
	}
	res := a.Auctioneer.Run(r.Context(), br)
	if res.Winner == nil {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	c, err := a.Store.GetCampaign(r.Context(), res.Winner.CampaignID)
	if err != nil {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	ok, err := bidding.Eligible(r.Context(), a.Store, *c, br.Country, br.UserID, time.Now())
	if err != nil || !ok {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	b, err := a.Store.FindBudget(r.Context(), c.ID)
	if err != nil || b == nil || !a.Pacer.Allow(c.ID, b.DailyCapMicros, res.ClearingMicros) {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	a.Pacer.Charge(c.ID, b.DailyCapMicros, res.ClearingMicros)
	httpx.WriteJSON(w, http.StatusOK, res)
}

// PreviewCreative handles GET /campaigns/{id}/creative and proxies the
// campaign's creative so the console can show it.
func (a *API) PreviewCreative(w http.ResponseWriter, r *http.Request) {
	c, ok := a.ownedCampaign(w, r)
	if !ok {
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	body, contentType, err := a.Client.OpenCreative(ctx, c.CreativeURL)
	if err != nil {
		httpx.WriteError(w, http.StatusBadGateway, "creative unavailable")
		return
	}
	defer body.Close()
	if !strings.HasPrefix(contentType, "image/") {
		httpx.WriteError(w, http.StatusBadGateway, "creative is not an image")
		return
	}
	w.Header().Set("Content-Type", contentType)
	if _, err := io.Copy(w, io.LimitReader(body, 2<<20)); err != nil {
		log.Printf("preview creative %d: copy: %v", c.ID, err)
	}
}

// GetBidSettings handles GET /campaigns/{id}/bidding.
func (a *API) GetBidSettings(w http.ResponseWriter, r *http.Request) {
	c, _ := r.Context().Value(httpx.CampaignKey{}).(*store.Campaign)
	httpx.WriteJSON(w, http.StatusOK, map[string]any{
		"campaign_id":    c.ID,
		"max_cpm_micros": c.MaxCPMMicros,
		"frequency_cap":  c.FrequencyCap,
		"countries":      c.Countries,
		"status":         c.Status,
	})
}

type impressionRequest struct {
	ImpressionID string `json:"impression_id"`
	RequestID    string `json:"request_id"`
	CampaignID   int64  `json:"campaign_id"`
	UserID       string `json:"user_id"`
	PriceMicros  int64  `json:"price_micros"`
}

// TrackImpression handles POST /impressions from the ad server once a won
// creative has rendered.
func (a *API) TrackImpression(w http.ResponseWriter, r *http.Request) {
	var req impressionRequest
	if err := httpx.DecodeJSON(w, r, a.Cfg.MaxBodyBytes, &req); err != nil {
		httpx.WriteError(w, http.StatusBadRequest, err.Error())
		return
	}
	if req.ImpressionID == "" || req.CampaignID <= 0 || req.PriceMicros <= 0 {
		httpx.WriteError(w, http.StatusBadRequest, "impression_id, campaign_id and price_micros are required")
		return
	}
	if err := a.Store.ReserveSpend(r.Context(), req.CampaignID, req.PriceMicros); err != nil {
		if errors.Is(err, store.ErrBudgetExhausted) {
			httpx.WriteError(w, http.StatusConflict, "campaign budget exhausted")
			return
		}
		httpx.WriteError(w, http.StatusInternalServerError, "could not reserve spend")
		return
	}
	ev := bidding.ImpressionEvent{
		Impression: store.Impression{ID: req.ImpressionID, CampaignID: req.CampaignID, UserID: req.UserID, PriceMicros: req.PriceMicros, ServedAt: time.Now().UTC()},
		Notice:     exchange.WinNotice{RequestID: req.RequestID, CampaignID: req.CampaignID, PriceMicros: req.PriceMicros},
	}
	if err := a.Recorder.Record(ev); err != nil {
		log.Printf("impression %s for campaign %d not queued: %v", req.ImpressionID, req.CampaignID, err)
	}
	w.WriteHeader(http.StatusAccepted)
}

// ListImpressions handles GET /campaigns/{id}/impressions.
func (a *API) ListImpressions(w http.ResponseWriter, r *http.Request) {
	c, ok := a.ownedCampaign(w, r)
	if !ok {
		return
	}
	page, size := httpx.ParsePage(r)
	imps, err := a.Store.ListImpressions(r.Context(), c.ID, page, size)
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "could not list impressions")
		return
	}
	httpx.WriteJSON(w, http.StatusOK, map[string]any{"page": page, "size": size, "impressions": imps})
}

// ExportImpressions handles GET /campaigns/{id}/impressions.csv?day=YYYY-MM-DD.
func (a *API) ExportImpressions(w http.ResponseWriter, r *http.Request) {
	c, ok := a.ownedCampaign(w, r)
	if !ok {
		return
	}
	day, err := time.Parse(time.DateOnly, r.URL.Query().Get("day"))
	if err != nil {
		httpx.WriteError(w, http.StatusBadRequest, "day must be YYYY-MM-DD")
		return
	}
	w.Header().Set("Content-Type", "text/csv")
	if err := a.Store.ExportImpressions(r.Context(), c.ID, day, w); err != nil {
		log.Printf("export impressions for campaign %d: %v", c.ID, err)
	}
}
