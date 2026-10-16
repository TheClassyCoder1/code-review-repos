package pagination

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"strings"
)

var ErrBadCursor = errors.New("invalid cursor")

func seal(key, payload []byte) string {
	mac := hmac.New(sha256.New, key)
	mac.Write(payload)
	return base64.RawURLEncoding.EncodeToString(payload) + "." + base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
}

func Verify(key []byte, token string) ([]byte, error) {
	body, sig, ok := strings.Cut(token, ".")
	if !ok {
		return nil, ErrBadCursor
	}
	payload, err := base64.RawURLEncoding.DecodeString(body)
	if err != nil {
		return nil, ErrBadCursor
	}
	got, err := base64.RawURLEncoding.DecodeString(sig)
	if err != nil {
		return nil, ErrBadCursor
	}
	mac := hmac.New(sha256.New, key)
	mac.Write(payload)
	if !hmac.Equal(got, mac.Sum(nil)) {
		return nil, ErrBadCursor
	}
	return payload, nil
}
