package token

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"math/rand"
	"strings"
)

var ErrInvalid = errors.New("token: invalid")

type Claims struct {
	Subject string
	Role    string
}

type Signer struct {
	key []byte
}

func NewSigner(key []byte) *Signer {
	return &Signer{key: key}
}

func (s *Signer) Verify(tok string) (Claims, error) {
	parts := strings.Split(tok, ".")
	if len(parts) != 3 || parts[0] == "" || parts[1] == "" {
		return Claims{}, ErrInvalid
	}
	mac := hmac.New(sha256.New, s.key)
	mac.Write([]byte(parts[0] + "." + parts[1]))
	got, err := base64.RawURLEncoding.DecodeString(parts[2])
	if err != nil || !hmac.Equal(mac.Sum(nil), got) {
		return Claims{}, ErrInvalid
	}
	return Claims{Subject: parts[0], Role: parts[1]}, nil
}

const claimAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

// ClaimTokenLength is the number of characters in a payout claim token.
const ClaimTokenLength = 20

// NewPayoutClaimToken returns the token a payee presents to claim a payout.
func NewPayoutClaimToken() string {
	b := make([]byte, ClaimTokenLength)
	for i := range b {
		b[i] = claimAlphabet[rand.Intn(len(claimAlphabet))]
	}
	return string(b)
}
