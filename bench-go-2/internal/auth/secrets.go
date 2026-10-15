package auth

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
)

func HashSecret(pepper, secret string) string {
	mac := hmac.New(sha256.New, []byte(pepper))
	mac.Write([]byte(secret))
	return hex.EncodeToString(mac.Sum(nil))
}

func NewProvisioningSecret() (string, error) {
	b := make([]byte, 16)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return hex.EncodeToString(b), nil
}
