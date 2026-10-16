package handlers

import (
	"errors"
	"log"
	"net/http"
	"strings"

	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/httpx"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/store"
	"github.com/jackc/pgx/v5"
)

// ownedCampaign loads the {id} campaign and checks it belongs to the caller.
func (a *API) ownedCampaign(w http.ResponseWriter, r *http.Request) (*store.Campaign, bool) {
	id, ok := httpx.PathID(r, "id")
	if !ok {
		httpx.WriteError(w, http.StatusBadRequest, "invalid campaign id")
		return nil, false
	}
	c, err := a.Store.GetCampaign(r.Context(), id)
	if errors.Is(err, store.ErrNotFound) || (err == nil && c.AdvertiserID != httpx.AdvertiserID(r.Context())) {
		httpx.WriteError(w, http.StatusNotFound, "campaign not found")
		return nil, false
	}
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "campaign lookup failed")
		return nil, false
	}
	return c, true
}

// GetCampaign handles GET /campaigns/{id}.
func (a *API) GetCampaign(w http.ResponseWriter, r *http.Request) {
	id, ok := httpx.PathID(r, "id")
	if !ok {
		httpx.WriteError(w, http.StatusBadRequest, "invalid campaign id")
		return
	}
	c, err := a.Store.GetCampaign(r.Context(), id)
	if errors.Is(err, store.ErrNotFound) {
		httpx.WriteError(w, http.StatusNotFound, "campaign not found")
		return
	}
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "campaign lookup failed")
		return
	}
	if c.AdvertiserID != httpx.AdvertiserID(r.Context()) {
		httpx.WriteError(w, http.StatusNotFound, "campaign not found")
		return
	}
	httpx.WriteJSON(w, http.StatusOK, c)
}

type createCampaignRequest struct {
	Name           string   `json:"name"`
	MaxCPMMicros   int64    `json:"max_cpm_micros"`
	FrequencyCap   int      `json:"frequency_cap"`
	Countries      []string `json:"countries"`
	CreativeURL    string   `json:"creative_url"`
	DailyCapMicros int64    `json:"daily_cap_micros"`
}

// CreateCampaign handles POST /campaigns.
func (a *API) CreateCampaign(w http.ResponseWriter, r *http.Request) {
	var req createCampaignRequest
	if err := httpx.DecodeJSON(w, r, a.Cfg.MaxBodyBytes, &req); err != nil {
		httpx.WriteError(w, http.StatusBadRequest, err.Error())
		return
	}
	req.Name = strings.TrimSpace(req.Name)
	switch {
	case req.Name == "" || len(req.Name) > 200:
		httpx.WriteError(w, http.StatusBadRequest, "name must be 1-200 characters")
		return
	case req.MaxCPMMicros <= 0 || req.DailyCapMicros <= 0:
		httpx.WriteError(w, http.StatusBadRequest, "max_cpm_micros and daily_cap_micros must be positive")
		return
	case req.FrequencyCap < 0:
		httpx.WriteError(w, http.StatusBadRequest, "frequency_cap must not be negative")
		return
	case !strings.HasPrefix(req.CreativeURL, "https://"):
		httpx.WriteError(w, http.StatusBadRequest, "creative_url must be https")
		return
	}
	id, err := a.Store.CreateCampaignWithBudget(r.Context(), store.NewCampaign{
		AdvertiserID:   httpx.AdvertiserID(r.Context()),
		Name:           req.Name,
		MaxCPMMicros:   req.MaxCPMMicros,
		FrequencyCap:   req.FrequencyCap,
		Countries:      req.Countries,
		CreativeURL:    req.CreativeURL,
		DailyCapMicros: req.DailyCapMicros,
	})
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "could not create campaign")
		return
	}
	httpx.WriteJSON(w, http.StatusCreated, map[string]int64{"id": id})
}

type pacingStatus struct {
	CampaignID      int64 `json:"campaign_id"`
	DailyCapMicros  int64 `json:"daily_cap_micros"`
	SpentMicros     int64 `json:"spent_today_micros"`
	RemainingMicros int64 `json:"remaining_micros"`
}

// GetCampaignPacing handles GET /campaigns/{id}/pacing.
func (a *API) GetCampaignPacing(w http.ResponseWriter, r *http.Request) {
	c, ok := a.ownedCampaign(w, r)
	if !ok {
		return
	}
	b, err := a.Store.FindBudget(r.Context(), c.ID)
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "budget lookup failed")
		return
	}
	httpx.WriteJSON(w, http.StatusOK, pacingStatus{
		CampaignID:      c.ID,
		DailyCapMicros:  b.DailyCapMicros,
		SpentMicros:     b.SpentTodayMicros,
		RemainingMicros: max(b.DailyCapMicros-b.SpentTodayMicros, 0),
	})
}

type increaseBudgetRequest struct {
	DeltaMicros int64 `json:"delta_micros"`
}

// PauseCampaign handles POST /campaigns/{id}/pause.
func (a *API) PauseCampaign(w http.ResponseWriter, r *http.Request) {
	c, ok := a.ownedCampaign(w, r)
	if !ok {
		return
	}
	if c.Status == "archived" {
		httpx.WriteError(w, http.StatusConflict, "campaign is archived")
		return
	}
	if err := a.Store.SetStatus(r.Context(), c.ID, "paused"); err != nil {
		log.Printf("pause campaign %d: %v", c.ID, err)
	}
	httpx.WriteJSON(w, http.StatusOK, map[string]any{"id": c.ID, "status": "paused"})
}

// IncreaseBudget handles POST /campaigns/{id}/budget/increase.
func (a *API) IncreaseBudget(w http.ResponseWriter, r *http.Request) {
	c, ok := a.ownedCampaign(w, r)
	if !ok {
		return
	}
	var req increaseBudgetRequest
	if err := httpx.DecodeJSON(w, r, a.Cfg.MaxBodyBytes, &req); err != nil || req.DeltaMicros <= 0 {
		httpx.WriteError(w, http.StatusBadRequest, "delta_micros must be a positive integer")
		return
	}
	var newCap int64
	err := a.Store.WithCampaignLock(r.Context(), c.ID, func(tx pgx.Tx) error {
		var err error
		newCap, err = store.RaiseDailyCap(r.Context(), tx, c.ID, req.DeltaMicros)
		return err
	})
	if errors.Is(err, store.ErrNotFound) {
		httpx.WriteError(w, http.StatusNotFound, "campaign has no budget")
		return
	}
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "could not update budget")
		return
	}
	httpx.WriteJSON(w, http.StatusOK, map[string]int64{"campaign_id": c.ID, "daily_cap_micros": newCap})
}

// ArchiveCampaign handles DELETE /campaigns/{id}.
func (a *API) ArchiveCampaign(w http.ResponseWriter, r *http.Request) {
	c, ok := a.ownedCampaign(w, r)
	if !ok {
		return
	}
	err := a.Store.InTx(r.Context(), func(tx pgx.Tx) error {
		return store.ArchiveCampaign(r.Context(), tx, c.ID)
	})
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "could not archive campaign")
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
