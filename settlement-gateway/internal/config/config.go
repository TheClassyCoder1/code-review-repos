package config

import (
	"os"
	"strconv"
	"time"
)

type Config struct {
	Addr         string
	DatabaseURL  string
	BankBaseURL  string
	FXBaseURL    string
	StatementDir string
	SigningKey   []byte
	HTTPTimeout  time.Duration
}

func getenv(key, fallback string) string {
	if v, ok := os.LookupEnv(key); ok && v != "" {
		return v
	}
	return fallback
}

func getenvInt(key string, fallback int) int {
	v, err := strconv.Atoi(os.Getenv(key))
	if err != nil || v <= 0 {
		return fallback
	}
	return v
}

func Load() Config {
	return Config{
		Addr:         getenv("ADDR", ":8080"),
		DatabaseURL:  os.Getenv("DATABASE_URL"),
		BankBaseURL:  getenv("BANK_BASE_URL", "https://bank.internal"),
		FXBaseURL:    getenv("FX_BASE_URL", "https://fx.internal"),
		StatementDir: getenv("STATEMENT_DIR", "/var/lib/settlement/statements"),
		SigningKey:   []byte(os.Getenv("SIGNING_KEY")),
		HTTPTimeout:  time.Duration(getenvInt("HTTP_TIMEOUT_SECONDS", 10)) * time.Second,
	}
}
