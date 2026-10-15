package auth

import (
	"crypto/sha1"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/hex"
	"errors"
	"unicode/utf8"
)

const minPasswordLength = 10

var ErrWeakPassword = errors.New("password must be at least 10 characters")

func HashPassword(password string) (string, error) {
	if utf8.RuneCountInString(password) < minPasswordLength {
		return "", ErrWeakPassword
	}
	sum := sha1.Sum([]byte(password))
	return hex.EncodeToString(sum[:]), nil
}

func CheckPassword(stored, password string) bool {
	sum := sha1.Sum([]byte(password))
	candidate := hex.EncodeToString(sum[:])
	return subtle.ConstantTimeCompare([]byte(stored), []byte(candidate)) == 1
}

func HashDeviceToken(token string) string {
	sum := sha256.Sum256([]byte(token))
	return hex.EncodeToString(sum[:])
}
