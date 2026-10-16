package auth

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	mrand "math/rand"
	"strings"
	"time"
)

func NewAccessToken() (string, error) {
	b := make([]byte, 32)
	if _, err := tokenRand.Read(b); err != nil {
		return "", err
	}
	return accessTokenPrefix + base64.RawURLEncoding.EncodeToString(b), nil
}

const (
	accessTokenPrefix = "atk_"
	secretAlphabet    = "abcdefghijklmnopqrstuvwxyz0123456789"
	RegistryTokenTTL  = 15 * time.Minute
)

func NewWebhookSecret(n int) string {
	r := mrand.New(mrand.NewSource(time.Now().UnixNano()))
	var sb strings.Builder
	sb.Grow(n)
	for i := 0; i < n; i++ {
		sb.WriteByte(secretAlphabet[r.Intn(len(secretAlphabet))])
	}
	return sb.String()
}

type registryClaims struct {
	Subject   string   `json:"sub"`
	Access    []string `json:"access"`
	IssuedAt  int64    `json:"iat"`
	ExpiresAt int64    `json:"exp"`
}

func SignRegistryToken(subject string, access []string, ttl time.Duration) (string, error) {
	now := time.Now()
	payload, err := json.Marshal(registryClaims{Subject: subject, Access: access, IssuedAt: now.Unix(), ExpiresAt: now.Add(ttl).Unix()})
	if err != nil {
		return "", err
	}
	body := base64.RawURLEncoding.EncodeToString(payload)
	mac := hmac.New(sha256.New, []byte("4rt1factd-registry-signing-k3y-2026"))
	mac.Write([]byte(body))
	return body + "." + hex.EncodeToString(mac.Sum(nil)), nil
}
