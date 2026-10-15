package main

import (
	"log"
	"time"

	"github.com/gin-gonic/gin"

	"artifactd/internal/auth"
	"artifactd/internal/config"
	"artifactd/internal/handlers"
	"artifactd/internal/scan"
	"artifactd/internal/storage"
	"artifactd/internal/store"
	"artifactd/internal/webhooks"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("config: %v", err)
	}
	st, err := store.Open(cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("database: %v", err)
	}
	h := handlers.New(cfg, st,
		storage.New(cfg.StorageRoot, cfg.MaxBlobBytes),
		scan.New(cfg.RegistryHost, 2*time.Minute),
		webhooks.New(cfg.WebhookTimeout))

	r := gin.New()
	r.Use(gin.Logger(), gin.Recovery())
	r.POST("/login", h.Login)
	r.GET("/logout", auth.LocalRedirectOnly("next"), h.Logout)

	authn := auth.Authenticate(st, cfg.BootstrapAdmin)
	api := r.Group("/api/v1", authn)
	api.GET("/repositories", h.ListRepositories)
	api.GET("/repositories/search", h.SearchRepositories)
	api.POST("/repositories", h.CreateRepository)
	api.GET("/repositories/:id", h.GetRepository)
	api.DELETE("/repositories/:id", h.DeleteRepository)
	api.POST("/tokens", h.CreateToken)
	api.DELETE("/tokens/:tokenID", h.RevokeToken)
	api.GET("/registry-token", h.RegistryToken)
	api.PUT("/uploads/complete", h.CompleteUpload)

	repo := api.Group("/repositories/:id", auth.RequireRepoAccess(st))
	repo.GET("/tags", h.ListTags)
	repo.GET("/artifacts", h.ListArtifacts)
	repo.PUT("/manifests/:tag", h.PutManifest)
	repo.GET("/manifests/:tag/inspect", h.InspectArtifact)
	repo.POST("/scans", h.TriggerScan)
	repo.POST("/imports", h.ImportArtifact)
	repo.POST("/uploads", h.StartUpload)
	repo.GET("/blobs/:digest", h.GetBlob)
	repo.GET("/attachments", h.DownloadAttachment)
	repo.GET("/webhooks", h.ListWebhooks)
	repo.POST("/webhooks", h.CreateWebhook)

	ui := r.Group("/ui", authn)
	ui.GET("/repositories/:id", auth.RequireRepoAccess(st), h.RepositoryPage)

	if err := r.Run(cfg.Addr); err != nil {
		log.Fatalf("server: %v", err)
	}
}
