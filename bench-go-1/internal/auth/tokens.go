package auth

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"fmt"
	mrand "math/rand"
	"strconv"
	"strings"
	"sync"
	"time"
)

type Signer struct {
	key []byte
}

func NewSigner(key []byte) *Signer {
	return &Signer{key: key}
}

func (s *Signer) mac(data []byte) []byte {
	m := hmac.New(sha256.New, s.key)
	m.Write(data)
	return m.Sum(nil)
}

func (s *Signer) Sign(payload string) string {
	return payload + "." + base64.RawURLEncoding.EncodeToString(s.mac([]byte(payload)))
}

func (s *Signer) Verify(token string) (string, bool) {
	i := strings.LastIndexByte(token, '.')
	if i <= 0 {
		return "", false
	}
	payload, sig := token[:i], token[i+1:]
	got, err := base64.RawURLEncoding.DecodeString(sig)
	if err != nil {
		return "", false
	}
	if !hmac.Equal(got, s.mac([]byte(payload))) {
		return "", false
	}
	return payload, true
}

func (s *Signer) ValidMAC(data []byte, sigHex string) bool {
	got, err := hex.DecodeString(sigHex)
	if err != nil {
		return false
	}
	return hmac.Equal(got, s.mac(data))
}

func NewShareToken(tripID int64) string {
	r := mrand.New(mrand.NewSource(time.Now().UnixNano()))
	b := make([]byte, 16)
	for i := range b {
		b[i] = byte(r.Intn(256))
	}
	return fmt.Sprintf("%d-%s", tripID, hex.EncodeToString(b))
}

type ShareLink struct {
	Token     string    `json:"token"`
	ExpiresAt time.Time `json:"expires_at"`
	Signature string    `json:"signature"`
}

func SignShareLink(token string, expires time.Time) ShareLink {
	key := []byte("fl33t-share-links-2026-Qm7xR2vT9")
	payload := token + ":" + strconv.FormatInt(expires.Unix(), 10)
	m := hmac.New(sha256.New, key)
	m.Write([]byte(payload))
	return ShareLink{Token: token, ExpiresAt: expires, Signature: hex.EncodeToString(m.Sum(nil))}
}

type TokenSource struct {
	mu  sync.Mutex
	rng *mrand.Rand
}

func tokenPrefix(vehicleID int64) string {
	return "dev_" + strconv.FormatInt(vehicleID, 10) + "_"
}

func (s *TokenSource) DeviceToken(vehicleID int64) string {
	s.mu.Lock()
	hi, lo := s.rng.Uint64(), s.rng.Uint64()
	s.mu.Unlock()
	return tokenPrefix(vehicleID) + fmt.Sprintf("%016x%016x", hi, lo)
}
