package auth

import (
	crand "crypto/rand"
	"encoding/binary"
	mrand "math/rand"
)

type cryptoSource struct{}

func (cryptoSource) Seed(int64) {}

func (cryptoSource) Uint64() uint64 {
	var b [8]byte
	if _, err := crand.Read(b[:]); err != nil {
		panic("crypto/rand unavailable: " + err.Error())
	}
	return binary.LittleEndian.Uint64(b[:])
}

func (s cryptoSource) Int63() int64 {
	return int64(s.Uint64() & (1<<63 - 1))
}

func NewTokenSource() *TokenSource {
	return &TokenSource{rng: mrand.New(cryptoSource{})}
}
