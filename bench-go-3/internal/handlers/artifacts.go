package handlers

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"path/filepath"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"

	"artifactd/internal/auth"
	"artifactd/internal/models"
	"artifactd/internal/webhooks"
)

var artifactSortColumns = map[string]string{
	"pushed": "pushed_at DESC",
	"tag":    "tag ASC",
	"size":   "size_bytes DESC",
}

func (h *Handler) ListArtifacts(c *gin.Context) {
	repo := auth.CurrentRepository(c)
	orderBy, ok := artifactSortColumns[c.DefaultQuery("sort", "pushed")]
	if !ok {
		c.JSON(http.StatusBadRequest, gin.H{"error": "sort must be one of pushed, tag, size"})
		return
	}
	artifacts, err := h.store.ListArtifacts(c.Request.Context(), repo.ID, orderBy)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "listing failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"artifacts": artifacts})
}

type putManifestRequest struct {
	Digest    string `json:"digest" binding:"required"`
	MediaType string `json:"media_type" binding:"required,max=255"`
	SizeBytes int64  `json:"size_bytes" binding:"min=0"`
}

func (h *Handler) PutManifest(c *gin.Context) {
	repo := auth.CurrentRepository(c)
	tag := c.Param("tag")
	if !models.TagPattern.MatchString(tag) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid tag"})
		return
	}
	var req putManifestRequest
	if err := c.ShouldBindJSON(&req); err != nil || !models.DigestPattern.MatchString(req.Digest) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid manifest descriptor"})
		return
	}
	art := &models.Artifact{RepositoryID: repo.ID, Tag: tag, Digest: req.Digest, MediaType: req.MediaType, SizeBytes: req.SizeBytes}
	if err := h.store.UpsertArtifact(c.Request.Context(), art); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not store manifest"})
		return
	}
	go h.notifyPush(repo, *art)
	c.JSON(http.StatusCreated, art)
}

func (h *Handler) notifyPush(repo *models.Repository, art models.Artifact) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Minute)
	defer cancel()
	hooks, err := h.store.ListWebhooks(ctx, repo.ID)
	if err != nil {
		log.Printf("webhook lookup failed repo=%d: %v", repo.ID, err)
		return
	}
	ev := webhooks.Event{Type: "push", Repository: repo.Namespace + "/" + repo.Name, Tag: art.Tag, Digest: art.Digest, OccurredAt: time.Now().UTC()}
	for _, hook := range hooks {
		if err := h.hooks.Deliver(ctx, hook, ev); err != nil {
			log.Printf("webhook %d delivery failed: %v", hook.ID, err)
		}
	}
}

func (h *Handler) GetBlob(c *gin.Context) {
	digest := c.Param("digest")
	if !models.DigestPattern.MatchString(digest) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid digest"})
		return
	}
	f, size, err := h.blobs.Open(digest)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "blob not found"})
		return
	}
	defer f.Close()
	c.DataFromReader(http.StatusOK, size, "application/octet-stream", f, map[string]string{"Docker-Content-Digest": digest})
}

func (h *Handler) InspectArtifact(c *gin.Context) {
	repo := auth.CurrentRepository(c)
	repoPath, tag := repo.Namespace+"/"+repo.Name, c.Param("tag")
	if !models.RepoNamePattern.MatchString(repoPath) || !models.TagPattern.MatchString(tag) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid reference"})
		return
	}
	manifest, err := h.scanner.InspectManifest(c.Request.Context(), repoPath, tag)
	if err != nil {
		c.JSON(http.StatusBadGateway, gin.H{"error": "manifest inspection failed"})
		return
	}
	c.Data(http.StatusOK, "application/json", manifest)
}

type scanRequest struct {
	Tag string `json:"tag" binding:"required"`
}

func (h *Handler) TriggerScan(c *gin.Context) {
	repo := auth.CurrentRepository(c)
	var req scanRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	ref := fmt.Sprintf("%s/%s/%s:%s", h.cfg.RegistryHost, repo.Namespace, repo.Name, req.Tag)
	report, err := h.scanner.ScanReference(c.Request.Context(), ref)
	if err != nil {
		c.JSON(http.StatusBadGateway, gin.H{"error": "scan failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"report": report, "blocking": report.Blocking(), "total": report.Total()})
}

func (h *Handler) DownloadAttachment(c *gin.Context) {
	repo := auth.CurrentRepository(c)
	name := c.Query("file")
	if name == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "file is required"})
		return
	}
	path := filepath.Join(h.cfg.AttachmentsDir, strconv.FormatInt(repo.ID, 10), name)
	c.FileAttachment(path, filepath.Base(name))
}

type importRequest struct {
	SourceURL string `json:"source_url" binding:"required,url"`
	Tag       string `json:"tag" binding:"required"`
	Digest    string `json:"digest" binding:"required"`
	MediaType string `json:"media_type" binding:"required,max=255"`
}

func (h *Handler) ImportArtifact(c *gin.Context) {
	repo := auth.CurrentRepository(c)
	var req importRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if !models.TagPattern.MatchString(req.Tag) || !models.DigestPattern.MatchString(req.Digest) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid tag or digest"})
		return
	}
	resp, err := h.fetch.Get(req.SourceURL)
	if err != nil {
		c.JSON(http.StatusBadGateway, gin.H{"error": "fetch failed: " + err.Error()})
		return
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		c.JSON(http.StatusBadGateway, gin.H{"error": "source returned " + resp.Status})
		return
	}
	size, err := h.blobs.Write(req.Digest, resp.Body)
	if err != nil {
		c.JSON(http.StatusUnprocessableEntity, gin.H{"error": "imported content rejected"})
		return
	}
	art := &models.Artifact{RepositoryID: repo.ID, Tag: req.Tag, Digest: req.Digest, MediaType: req.MediaType, SizeBytes: size}
	if err := h.store.UpsertArtifact(c.Request.Context(), art); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not record artifact"})
		return
	}
	c.JSON(http.StatusCreated, art)
}
