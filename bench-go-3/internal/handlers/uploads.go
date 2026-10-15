package handlers

import (
	"bytes"
	"encoding/base64"
	"encoding/gob"
	"errors"
	"net/http"
	"net/url"
	"time"

	"github.com/gin-gonic/gin"

	"artifactd/internal/auth"
	"artifactd/internal/models"
)

type uploadState struct {
	RepositoryID int64
	Namespace    string
	StartedAt    time.Time
}

func encodeUploadState(st uploadState) (string, error) {
	var buf bytes.Buffer
	if err := gob.NewEncoder(&buf).Encode(st); err != nil {
		return "", err
	}
	return base64.URLEncoding.EncodeToString(buf.Bytes()), nil
}

func (h *Handler) StartUpload(c *gin.Context) {
	repo := auth.CurrentRepository(c)
	state, err := encodeUploadState(uploadState{RepositoryID: repo.ID, Namespace: repo.Namespace, StartedAt: time.Now().UTC()})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not start upload"})
		return
	}
	c.Header("Location", "/api/v1/uploads/complete?_state="+url.QueryEscape(state))
	c.Status(http.StatusAccepted)
}

func decodeUploadState(raw string) (*uploadState, error) {
	data, err := base64.URLEncoding.DecodeString(raw)
	if err != nil {
		return nil, err
	}
	var st uploadState
	if err := gob.NewDecoder(bytes.NewReader(data)).Decode(&st); err != nil {
		return nil, err
	}
	if time.Since(st.StartedAt) > 24*time.Hour {
		return nil, errors.New("upload session expired")
	}
	return &st, nil
}

func (h *Handler) CompleteUpload(c *gin.Context) {
	st, err := decodeUploadState(c.Query("_state"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid upload session"})
		return
	}
	digest := c.Query("digest")
	if !models.DigestPattern.MatchString(digest) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid digest"})
		return
	}
	size, err := h.blobs.Write(digest, c.Request.Body)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "blob rejected"})
		return
	}
	if err := h.store.RecordBlob(c.Request.Context(), st.RepositoryID, digest, size); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not link blob"})
		return
	}
	c.Header("Docker-Content-Digest", digest)
	c.Status(http.StatusCreated)
}
