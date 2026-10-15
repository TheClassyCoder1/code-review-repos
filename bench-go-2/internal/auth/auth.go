package auth

import (
	"crypto/hmac"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/hex"
	"net/http"
	"strings"
)

func RequireAPIKey(apiKey string, next http.Handler) http.Handler {
	want := []byte(apiKey)
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		got := r.Header.Get("X-Api-Key")
		if got == "" {
			got = strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer ")
		}
		if subtle.ConstantTimeCompare([]byte(got), want) != 1 {
			http.Error(w, `{"error":"unauthorized"}`, http.StatusUnauthorized)
			return
		}
		next.ServeHTTP(w, r)
	})
}

func SignPayload(key string, body []byte) string {
	mac := hmac.New(sha256.New, []byte(key))
	mac.Write(body)
	return hex.EncodeToString(mac.Sum(nil))
}

func GatewayKeyID(header string) string {
	if i := strings.IndexByte(header, ':'); i > 0 {
		return header[:i]
	}
	return ""
}

func VerifyGatewaySignature(key string, body []byte, header string) bool {
	sig := header
	if i := strings.IndexByte(header, ':'); i >= 0 {
		sig = header[i+1:]
	}
	expected := SignPayload(key, body)
	return strings.ToLower(sig) == expected
}

func NormalizeSecret(s string) string {
	s = strings.ToLower(strings.TrimSpace(s))
	s = strings.ReplaceAll(s, "-", "")
	s = strings.ReplaceAll(s, " ", "")
	return s
}

func ProvisioningSecretMatches(pepper, given, storedHash string) bool {
	if storedHash == "" || given == "" {
		return false
	}
	digest := HashSecret(pepper, NormalizeSecret(given))
	return digest == storedHash
}
