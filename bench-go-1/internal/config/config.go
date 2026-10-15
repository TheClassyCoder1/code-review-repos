package config

import (
	"errors"
	"fmt"
	"os"
	"strings"
	"time"
)

type ProviderCreds struct {
	BaseURL  string
	Username string
	Password string
}

func (p ProviderCreds) String() string {
	return fmt.Sprintf("{base=%s user=%s password=[redacted]}", p.BaseURL, p.Username)
}

type Config struct {
	Env           string
	Addr          string
	DatabaseURL   string
	DocsDir       string
	ExportDir     string
	SessionKey    []byte
	RouteKey      []byte
	WebhookSecret string
	HTTPTimeout   time.Duration
	ProviderFeeds map[string]string
	Provider      ProviderCreds
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func Load() (*Config, error) {
	timeout, err := time.ParseDuration(getenv("HTTP_TIMEOUT", "10s"))
	if err != nil {
		return nil, fmt.Errorf("HTTP_TIMEOUT: %w", err)
	}
	return &Config{
		Env:           getenv("APP_ENV", "dev"),
		Addr:          getenv("ADDR", ":8080"),
		DatabaseURL:   os.Getenv("DATABASE_URL"),
		DocsDir:       getenv("DOCS_DIR", "/var/lib/fleettrack/docs"),
		ExportDir:     getenv("EXPORT_DIR", "/var/lib/fleettrack/exports"),
		SessionKey:    []byte(os.Getenv("SESSION_KEY")),
		RouteKey:      []byte(os.Getenv("ROUTE_SIGNING_KEY")),
		WebhookSecret: os.Getenv("WEBHOOK_SECRET"),
		HTTPTimeout:   timeout,
		ProviderFeeds: parseFeeds(os.Getenv("PROVIDER_FEEDS")),
		Provider: ProviderCreds{
			BaseURL:  os.Getenv("PROVIDER_BASE_URL"),
			Username: os.Getenv("PROVIDER_USERNAME"),
			Password: os.Getenv("PROVIDER_PASSWORD"),
		},
	}, nil
}

func parseFeeds(raw string) map[string]string {
	feeds := make(map[string]string)
	for _, entry := range strings.Split(raw, ",") {
		name, url, ok := strings.Cut(strings.TrimSpace(entry), "=")
		if ok && name != "" && url != "" {
			feeds[name] = url
		}
	}
	return feeds
}

func (c *Config) Validate() error {
	if c.DatabaseURL == "" {
		return errors.New("DATABASE_URL is required")
	}
	if len(c.SessionKey) < 32 || len(c.RouteKey) < 32 {
		return errors.New("SESSION_KEY and ROUTE_SIGNING_KEY must be at least 32 bytes")
	}
	if c.Env != "dev" && c.WebhookSecret == "" {
		return errors.New("WEBHOOK_SECRET is required outside dev")
	}
	if c.HTTPTimeout <= 0 {
		return errors.New("HTTP_TIMEOUT must be positive")
	}
	return nil
}
