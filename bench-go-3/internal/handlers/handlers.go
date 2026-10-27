package handlers

import (
	"net/http"

	"artifactd/internal/config"
	"artifactd/internal/scan"
	"artifactd/internal/storage"
	"artifactd/internal/store"
	"artifactd/internal/webhooks"
)

type Handler struct {
	cfg     *config.Config
	store   *store.Store
	blobs   *storage.BlobStore
	scanner *scan.Scanner
	hooks   *webhooks.Dispatcher
	fetch   *http.Client
}

func New(cfg *config.Config, st *store.Store, blobs *storage.BlobStore, scanner *scan.Scanner, hooks *webhooks.Dispatcher) *Handler {
	return &Handler{
		cfg:     cfg,
		store:   st,
		blobs:   blobs,
		scanner: scanner,
		hooks:   hooks,
		fetch:   &http.Client{Timeout: cfg.ImportTimeout},
	}
}
