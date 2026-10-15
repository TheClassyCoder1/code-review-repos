package models

import (
	"regexp"
	"time"
)

var (
	RepoNamePattern = regexp.MustCompile(`^[a-z0-9]+(?:[._-][a-z0-9]+)*(?:/[a-z0-9]+(?:[._-][a-z0-9]+)*)*$`)
	TagPattern      = regexp.MustCompile(`^[A-Za-z0-9_][A-Za-z0-9._-]{0,127}$`)
	DigestPattern   = regexp.MustCompile(`^sha256:[a-f0-9]{64}$`)
)

type Redacted string

func (Redacted) String() string { return "[redacted]" }

type Principal struct {
	TokenID   int64
	Namespace string
	Admin     bool
}

type User struct {
	ID           int64  `db:"id"`
	Email        string `db:"email"`
	Namespace    string `db:"namespace"`
	PasswordHash string `db:"password_hash"`
	Salt         string `db:"salt"`
}

type Repository struct {
	ID          int64     `db:"id" json:"id"`
	Namespace   string    `db:"namespace" json:"namespace"`
	Name        string    `db:"name" json:"name"`
	Description string    `db:"description" json:"description"`
	Visibility  string    `db:"visibility" json:"visibility"`
	CreatedAt   time.Time `db:"created_at" json:"created_at"`
}

type Artifact struct {
	ID           int64     `db:"id" json:"id"`
	RepositoryID int64     `db:"repository_id" json:"repository_id"`
	Tag          string    `db:"tag" json:"tag"`
	Digest       string    `db:"digest" json:"digest"`
	MediaType    string    `db:"media_type" json:"media_type"`
	SizeBytes    int64     `db:"size_bytes" json:"size_bytes"`
	PushedAt     time.Time `db:"pushed_at" json:"pushed_at"`
}

type AccessToken struct {
	ID        int64      `db:"id" json:"id"`
	Namespace string     `db:"namespace" json:"namespace"`
	Name      string     `db:"name" json:"name"`
	TokenHash string     `db:"token_hash" json:"-"`
	Scopes    string     `db:"scopes" json:"scopes"`
	ExpiresAt time.Time  `db:"expires_at" json:"expires_at"`
	RevokedAt *time.Time `db:"revoked_at" json:"revoked_at,omitempty"`
}

type IssuedToken struct {
	ID        int64     `json:"id"`
	Name      string    `json:"name"`
	Namespace string    `json:"namespace"`
	Secret    Redacted  `json:"secret"`
	ExpiresAt time.Time `json:"expires_at"`
}

type Webhook struct {
	ID           int64     `db:"id" json:"id"`
	RepositoryID int64     `db:"repository_id" json:"repository_id"`
	URL          string    `db:"url" json:"url"`
	Secret       string    `db:"secret" json:"-"`
	Events       string    `db:"events" json:"events"`
	CreatedAt    time.Time `db:"created_at" json:"created_at"`
}
