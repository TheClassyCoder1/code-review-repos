package config

import (
	"errors"
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	Env                 string
	Addr                string
	DatabaseURL         string
	StorageRoot         string
	AttachmentsDir      string
	RegistryHost        string
	CursorKey           []byte
	WebhookAllowedHosts []string
	WebhookTimeout      time.Duration
	ImportTimeout       time.Duration
	BootstrapAdmin      bool
	MaxBlobBytes        int64
}

func Load() (*Config, error) {
	c := &Config{
		Env:            getenv("ARTIFACTD_ENV", "production"),
		Addr:           getenv("ARTIFACTD_ADDR", ":5080"),
		DatabaseURL:    os.Getenv("DATABASE_URL"),
		StorageRoot:    getenv("STORAGE_ROOT", "/var/lib/artifactd"),
		AttachmentsDir: getenv("ATTACHMENTS_DIR", "/var/lib/artifactd/attachments"),
		RegistryHost:   getenv("REGISTRY_HOST", "localhost:5080"),
		CursorKey:      []byte(os.Getenv("CURSOR_KEY")),
		BootstrapAdmin: os.Getenv("BOOTSTRAP_ADMIN") == "1",
	}
	for _, h := range strings.Split(os.Getenv("WEBHOOK_ALLOWED_HOSTS"), ",") {
		if h = strings.TrimSpace(h); h != "" {
			c.WebhookAllowedHosts = append(c.WebhookAllowedHosts, strings.ToLower(h))
		}
	}
	var err error
	if c.WebhookTimeout, err = time.ParseDuration(getenv("WEBHOOK_TIMEOUT", "10s")); err != nil {
		return nil, err
	}
	if c.ImportTimeout, err = time.ParseDuration(getenv("IMPORT_TIMEOUT", "5m")); err != nil {
		return nil, err
	}
	if c.MaxBlobBytes, err = strconv.ParseInt(getenv("MAX_BLOB_BYTES", "2147483648"), 10, 64); err != nil {
		return nil, err
	}
	return c, c.Validate()
}

func getenv(key, fallback string) string {
	if v, ok := os.LookupEnv(key); ok && v != "" {
		return v
	}
	return fallback
}

func (c *Config) Validate() error {
	if c.DatabaseURL == "" {
		return errors.New("DATABASE_URL is required")
	}
	if len(c.CursorKey) < 32 {
		return errors.New("CURSOR_KEY must be at least 32 bytes")
	}
	if c.BootstrapAdmin && c.Env != "local" {
		return errors.New("BOOTSTRAP_ADMIN is only allowed when ARTIFACTD_ENV=local")
	}
	if c.MaxBlobBytes <= 0 {
		return errors.New("MAX_BLOB_BYTES must be positive")
	}
	return nil
}
