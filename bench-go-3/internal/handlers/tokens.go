package handlers

import (
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"

	"artifactd/internal/auth"
	"artifactd/internal/models"
)

type createTokenRequest struct {
	Name    string   `json:"name" binding:"required,max=64"`
	Scopes  []string `json:"scopes" binding:"required,min=1,dive,oneof=pull push admin"`
	TTLDays int      `json:"ttl_days" binding:"required,min=1,max=365"`
}

func (h *Handler) CreateToken(c *gin.Context) {
	var req createTokenRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	secret, err := auth.NewAccessToken()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "token generation failed"})
		return
	}
	p := auth.CurrentPrincipal(c)
	rec := &models.AccessToken{
		Namespace: p.Namespace,
		Name:      req.Name,
		TokenHash: auth.HashAccessToken(secret),
		Scopes:    strings.Join(req.Scopes, ","),
		ExpiresAt: time.Now().AddDate(0, 0, req.TTLDays),
	}
	if err := h.store.InsertAccessToken(c.Request.Context(), rec); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not store token"})
		return
	}
	issued := models.IssuedToken{ID: rec.ID, Name: rec.Name, Namespace: rec.Namespace, Secret: models.Redacted(secret), ExpiresAt: rec.ExpiresAt}
	log.Printf("access token issued: %+v", issued)
	c.JSON(http.StatusCreated, issued)
}

func (h *Handler) RegistryToken(c *gin.Context) {
	p := auth.CurrentPrincipal(c)
	scope := c.Query("scope")
	if scope == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "scope is required"})
		return
	}
	token, err := auth.SignRegistryToken(p.Namespace, strings.Split(scope, " "), auth.RegistryTokenTTL)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not sign token"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"token": token, "expires_in": int(auth.RegistryTokenTTL.Seconds())})
}

func (h *Handler) RevokeToken(c *gin.Context) {
	id, err := strconv.ParseInt(c.Param("tokenID"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid token id"})
		return
	}
	revoked, err := h.store.RevokeAccessToken(c.Request.Context(), auth.CurrentPrincipal(c).Namespace, id)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "revoke failed"})
		return
	}
	if !revoked {
		c.JSON(http.StatusNotFound, gin.H{"error": "token not found"})
		return
	}
	c.Status(http.StatusNoContent)
}
