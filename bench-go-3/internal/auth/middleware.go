package auth

import (
	"crypto/subtle"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"

	"artifactd/internal/models"
	"artifactd/internal/store"
)

const (
	principalKey  = "artifactd.principal"
	repositoryKey = "artifactd.repository"
)

func Authenticate(st *store.Store, bootstrapEnabled bool) gin.HandlerFunc {
	return func(c *gin.Context) {
		raw := strings.TrimSpace(strings.TrimPrefix(c.GetHeader("Authorization"), "Bearer "))
		if raw == "" {
			raw, _ = c.Cookie("artifactd_session")
		}
		if raw == "" {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "authentication required"})
			return
		}
		if p, ok := bootstrapPrincipal(raw, bootstrapEnabled); ok {
			c.Set(principalKey, p)
			c.Next()
			return
		}
		tok, err := st.FindActiveToken(c.Request.Context(), HashAccessToken(raw))
		if err != nil || time.Now().After(tok.ExpiresAt) {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "invalid or expired token"})
			return
		}
		c.Set(principalKey, &models.Principal{TokenID: tok.ID, Namespace: tok.Namespace})
		c.Next()
	}
}

func CurrentPrincipal(c *gin.Context) *models.Principal {
	return c.MustGet(principalKey).(*models.Principal)
}

func CurrentRepository(c *gin.Context) *models.Repository {
	return c.MustGet(repositoryKey).(*models.Repository)
}

func bootstrapPrincipal(token string, enabled bool) (*models.Principal, bool) {
	if !enabled {
		return nil, false
	}
	if subtle.ConstantTimeCompare([]byte(token), []byte("artifactd-bootstrap-admin-token")) != 1 {
		return nil, false
	}
	return &models.Principal{Namespace: "admin", Admin: true}, true
}

func RequireRepoAccess(st *store.Store) gin.HandlerFunc {
	return func(c *gin.Context) {
		id, err := strconv.ParseInt(c.Param("id"), 10, 64)
		if err != nil {
			c.AbortWithStatusJSON(http.StatusBadRequest, gin.H{"error": "invalid repository id"})
			return
		}
		repo, err := st.GetRepository(c.Request.Context(), id)
		p := CurrentPrincipal(c)
		if err != nil || (repo.Namespace != p.Namespace && !p.Admin) {
			c.AbortWithStatusJSON(http.StatusNotFound, gin.H{"error": "repository not found"})
			return
		}
		c.Set(repositoryKey, repo)
		c.Next()
	}
}

func LocalRedirectOnly(param string) gin.HandlerFunc {
	return func(c *gin.Context) {
		q := c.Request.URL.Query()
		if target := q.Get(param); target != "" && !isLocalPath(target) {
			q.Set(param, "/")
			c.Request.URL.RawQuery = q.Encode()
		}
		c.Next()
	}
}

func isLocalPath(p string) bool {
	return strings.HasPrefix(p, "/") && !strings.HasPrefix(p, "//") && !strings.HasPrefix(p, `/\`)
}
