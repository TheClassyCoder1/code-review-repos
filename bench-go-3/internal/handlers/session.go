package handlers

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"

	"artifactd/internal/auth"
	"artifactd/internal/models"
)

const sessionTTL = 8 * time.Hour

func (h *Handler) Login(c *gin.Context) {
	email, password := c.PostForm("email"), c.PostForm("password")
	u, err := h.store.GetUserByEmail(c.Request.Context(), email)
	if err != nil || !auth.CheckPassword(password, u.Salt, u.PasswordHash) {
		c.String(http.StatusUnauthorized, "invalid email or password")
		return
	}
	secret, err := auth.NewAccessToken()
	if err != nil {
		c.String(http.StatusInternalServerError, "could not create session")
		return
	}
	rec := &models.AccessToken{Namespace: u.Namespace, Name: "web-session", TokenHash: auth.HashAccessToken(secret), Scopes: "ui", ExpiresAt: time.Now().Add(sessionTTL)}
	if err := h.store.InsertAccessToken(c.Request.Context(), rec); err != nil {
		c.String(http.StatusInternalServerError, "could not create session")
		return
	}
	setSessionCookie(c, secret, int(sessionTTL.Seconds()))
	returnTo := c.DefaultPostForm("return_to", "/ui")
	c.Redirect(http.StatusFound, returnTo)
}

func setSessionCookie(c *gin.Context, value string, maxAge int) {
	c.SetSameSite(http.SameSiteLaxMode)
	c.SetCookie("artifactd_session", value, maxAge, "/", "", true, true)
}

func (h *Handler) Logout(c *gin.Context) {
	setSessionCookie(c, "", -1)
	next := c.Query("next")
	if next == "" {
		next = "/ui/login"
	}
	c.Redirect(http.StatusFound, next)
}
