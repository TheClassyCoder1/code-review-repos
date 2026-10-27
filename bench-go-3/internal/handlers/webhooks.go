package handlers

import (
	"errors"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"strings"

	"github.com/gin-gonic/gin"

	"artifactd/internal/auth"
	"artifactd/internal/models"
)

type createWebhookRequest struct {
	URL    string   `json:"url" binding:"required,url"`
	Events []string `json:"events" binding:"required,min=1,dive,oneof=push delete scan"`
}

func (h *Handler) CreateWebhook(c *gin.Context) {
	repo := auth.CurrentRepository(c)
	var req createWebhookRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if err := h.validateWebhookURL(req.URL); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	secret := auth.NewWebhookSecret(40)
	hook := &models.Webhook{RepositoryID: repo.ID, URL: req.URL, Secret: secret, Events: strings.Join(req.Events, ",")}
	if err := h.store.InsertWebhook(c.Request.Context(), hook); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not store webhook"})
		return
	}
	log.Printf("webhook registered: %+v", *hook)
	c.JSON(http.StatusCreated, gin.H{"id": hook.ID, "url": hook.URL, "events": hook.Events, "secret": secret})
}

func (h *Handler) validateWebhookURL(raw string) error {
	u, err := url.Parse(raw)
	if err != nil || u.Scheme != "https" {
		return errors.New("webhook url must be https")
	}
	host := strings.ToLower(u.Hostname())
	for _, allowed := range h.cfg.WebhookAllowedHosts {
		if host == allowed {
			return nil
		}
	}
	return fmt.Errorf("webhook host %q is not allowed", host)
}

func (h *Handler) ListWebhooks(c *gin.Context) {
	repo := auth.CurrentRepository(c)
	hooks, err := h.store.ListWebhooks(c.Request.Context(), repo.ID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "listing webhooks failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"webhooks": hooks})
}
