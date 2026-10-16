package config

import (
	"errors"
	"os"
	"strconv"
	"strings"
	"time"
)

// Config is the runtime configuration of adbidd, read from the environment.
type Config struct {
	Addr             string
	DatabaseURL      string
	ReadTimeout      time.Duration
	WriteTimeout     time.Duration
	BidTimeout       time.Duration
	MaxBidBytes      int64
	MaxBodyBytes     int64
	Workers          int
	QueueSize        int
	FlushInterval    time.Duration
	ConversionSecret string
	AdminKeyDigest   string
	BidderURLs       []string
	WinNoticeURL     string
}

// Load reads the configuration from environment variables with defaults.
func Load() (Config, error) {
	c := Config{
		Addr:             getenv("ADBIDD_ADDR", ":8080"),
		DatabaseURL:      os.Getenv("DATABASE_URL"),
		ConversionSecret: os.Getenv("CONVERSION_SECRET"),
		AdminKeyDigest:   strings.ToLower(os.Getenv("ADMIN_KEY_SHA256")),
		WinNoticeURL:     os.Getenv("WIN_NOTICE_URL"),
	}
	var err error
	if c.ReadTimeout, err = getDuration("READ_TIMEOUT", 5*time.Second); err != nil {
		return Config{}, err
	}
	if c.WriteTimeout, err = getDuration("WRITE_TIMEOUT", 10*time.Second); err != nil {
		return Config{}, err
	}
	if c.BidTimeout, err = getDuration("BID_TIMEOUT", 120*time.Millisecond); err != nil {
		return Config{}, err
	}
	if c.FlushInterval, err = getDuration("FLUSH_INTERVAL", 10*time.Second); err != nil {
		return Config{}, err
	}
	if c.Workers, err = getInt("WORKERS", 8); err != nil {
		return Config{}, err
	}
	if c.QueueSize, err = getInt("QUEUE_SIZE", 4096); err != nil {
		return Config{}, err
	}
	c.MaxBidBytes = 64 << 10
	c.MaxBodyBytes = 1 << 20
	for _, u := range strings.Split(os.Getenv("BIDDER_URLS"), ",") {
		if u = strings.TrimSpace(u); u != "" {
			c.BidderURLs = append(c.BidderURLs, u)
		}
	}
	if c.DatabaseURL == "" {
		return Config{}, errors.New("DATABASE_URL is required")
	}
	if c.Workers < 1 || c.QueueSize < 1 {
		return Config{}, errors.New("WORKERS and QUEUE_SIZE must be positive")
	}
	return c, nil
}

func getenv(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

func getDuration(key string, def time.Duration) (time.Duration, error) {
	v := os.Getenv(key)
	if v == "" {
		return def, nil
	}
	return time.ParseDuration(v)
}

func getInt(key string, def int) (int, error) {
	v := os.Getenv(key)
	if v == "" {
		return def, nil
	}
	return strconv.Atoi(v)
}
