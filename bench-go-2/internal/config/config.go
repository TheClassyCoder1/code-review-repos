package config

import (
	"fmt"
	"os"
	"strconv"
	"time"
)

type Config struct {
	Addr           string
	DatabaseURL    string
	APIKey         string
	SecretPepper   string
	GatewayBaseURL string
	GatewayToken   string
	GatewayTimeout time.Duration
	RecalcTimeout  time.Duration
	MaxBodyBytes   int64
	MaxTariffBytes int64
	IngestWorkers  int
}

func Load() (*Config, error) {
	c := &Config{
		Addr:           getenv("ADDR", ":8080"),
		DatabaseURL:    os.Getenv("DATABASE_URL"),
		APIKey:         os.Getenv("API_KEY"),
		SecretPepper:   os.Getenv("SECRET_PEPPER"),
		GatewayBaseURL: getenv("GATEWAY_BASE_URL", "http://meter-gateway:9000"),
		GatewayToken:   os.Getenv("GATEWAY_TOKEN"),
	}
	for name, v := range map[string]string{"DATABASE_URL": c.DatabaseURL, "API_KEY": c.APIKey, "SECRET_PEPPER": c.SecretPepper} {
		if v == "" {
			return nil, fmt.Errorf("%s is required", name)
		}
	}
	var err error
	if c.GatewayTimeout, err = durationEnv("GATEWAY_TIMEOUT", 10*time.Second); err != nil {
		return nil, err
	}
	if c.RecalcTimeout, err = durationEnv("RECALC_TIMEOUT", 30*time.Second); err != nil {
		return nil, err
	}
	if c.MaxBodyBytes, err = intEnv("MAX_BODY_BYTES", 64<<10); err != nil {
		return nil, err
	}
	if c.MaxTariffBytes, err = intEnv("MAX_TARIFF_BYTES", 1<<20); err != nil {
		return nil, err
	}
	workers, err := intEnv("INGEST_WORKERS", 4)
	if err != nil {
		return nil, err
	}
	c.IngestWorkers = int(workers)
	return c, nil
}

func getenv(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

func durationEnv(key string, def time.Duration) (time.Duration, error) {
	v := os.Getenv(key)
	if v == "" {
		return def, nil
	}
	d, err := time.ParseDuration(v)
	if err != nil {
		return 0, fmt.Errorf("%s: %w", key, err)
	}
	return d, nil
}

func intEnv(key string, def int64) (int64, error) {
	v := os.Getenv(key)
	if v == "" {
		return def, nil
	}
	n, err := strconv.ParseInt(v, 10, 64)
	if err != nil || n <= 0 {
		return 0, fmt.Errorf("%s must be a positive integer", key)
	}
	return n, nil
}
