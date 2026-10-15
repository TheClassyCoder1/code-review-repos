package handlers

import (
	"errors"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"

	"artifactd/internal/auth"
	"artifactd/internal/models"
	"artifactd/internal/pagination"
	"artifactd/internal/store"
)

const repositoryPageSize = 50

func (h *Handler) GetRepository(c *gin.Context) {
	id, ok := repoID(c)
	if !ok {
		return
	}
	repo, err := h.store.GetRepository(c.Request.Context(), id)
	if errors.Is(err, store.ErrNotFound) || (err == nil && repo.Namespace != auth.CurrentPrincipal(c).Namespace) {
		c.JSON(http.StatusNotFound, gin.H{"error": "repository not found"})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "repository lookup failed"})
		return
	}
	c.JSON(http.StatusOK, repo)
}

func (h *Handler) ListRepositories(c *gin.Context) {
	var afterID int64
	if token := c.Query("cursor"); token != "" {
		raw, err := pagination.Verify(h.cfg.CursorKey, token)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid cursor"})
			return
		}
		cur, err := pagination.Decode(raw)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid cursor"})
			return
		}
		afterID = cur.AfterID
	}
	repos, err := h.store.ListRepositories(c.Request.Context(), auth.CurrentPrincipal(c).Namespace, afterID, repositoryPageSize)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "listing failed"})
		return
	}
	resp := gin.H{"repositories": repos}
	if len(repos) == repositoryPageSize {
		if next, err := pagination.Encode(h.cfg.CursorKey, pagination.Cursor{AfterID: repos[len(repos)-1].ID, PageSize: repositoryPageSize}); err == nil {
			resp["next_cursor"] = next
		}
	}
	c.JSON(http.StatusOK, resp)
}

type createRepositoryRequest struct {
	Name        string `json:"name" binding:"required,max=255"`
	Description string `json:"description" binding:"max=2000"`
	Visibility  string `json:"visibility" binding:"omitempty,oneof=private public"`
}

func (h *Handler) CreateRepository(c *gin.Context) {
	var req createRepositoryRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if !models.RepoNamePattern.MatchString(req.Name) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid repository name"})
		return
	}
	if req.Visibility == "" {
		req.Visibility = "private"
	}
	repo := &models.Repository{
		Namespace:   auth.CurrentPrincipal(c).Namespace,
		Name:        req.Name,
		Description: req.Description,
		Visibility:  req.Visibility,
	}
	if err := h.store.CreateRepository(c.Request.Context(), repo); err != nil {
		c.JSON(http.StatusConflict, gin.H{"error": "repository could not be created"})
		return
	}
	c.JSON(http.StatusCreated, repo)
}

func (h *Handler) SearchRepositories(c *gin.Context) {
	q := c.Query("q")
	if q == "" || len(q) > 100 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "q must be 1-100 characters"})
		return
	}
	repos, err := h.store.SearchRepositories(c.Request.Context(), auth.CurrentPrincipal(c).Namespace, q, 25)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "search failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"repositories": repos})
}

func (h *Handler) DeleteRepository(c *gin.Context) {
	id, ok := repoID(c)
	if !ok {
		return
	}
	if _, err := h.store.GetRepository(c.Request.Context(), id); err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "repository not found"})
		return
	}
	if err := h.store.DeleteRepository(c.Request.Context(), id); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "delete failed"})
		return
	}
	c.Status(http.StatusNoContent)
}

func repoID(c *gin.Context) (int64, bool) {
	id, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil || id <= 0 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid repository id"})
		return 0, false
	}
	return id, true
}

func (h *Handler) ListTags(c *gin.Context) {
	id, ok := repoID(c)
	if !ok {
		return
	}
	tags, err := h.store.ListTags(c.Request.Context(), id)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "listing tags failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"tags": tags})
}
