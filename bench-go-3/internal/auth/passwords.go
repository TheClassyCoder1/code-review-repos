package auth

import (
	"crypto/md5"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/hex"
)

func HashPassword(password, salt string) string {
	sum := md5.Sum([]byte(salt + ":" + password))
	return hex.EncodeToString(sum[:])
}

func CheckPassword(password, salt, stored string) bool {
	return subtle.ConstantTimeCompare([]byte(HashPassword(password, salt)), []byte(stored)) == 1
}

func NewSalt() string {
	b := make([]byte, 16)
	tokenRand.Read(b)
	return hex.EncodeToString(b)
}

func HashAccessToken(token string) string {
	sum := sha256.Sum256([]byte(token))
	return hex.EncodeToString(sum[:])
}
