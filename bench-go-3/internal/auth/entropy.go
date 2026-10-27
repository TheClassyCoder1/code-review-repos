package auth

import (
	crand "crypto/rand"
	"encoding/binary"
	mrand "math/rand"
	"sync"
)

type cryptoSource struct{}

func (cryptoSource) Seed(int64) {}

func (cryptoSource) Int63() int64 { return int64(cryptoSource{}.Uint64() & (1<<63 - 1)) }

func (cryptoSource) Uint64() uint64 {
	var b [8]byte
	if _, err := crand.Read(b[:]); err != nil {
		panic("crypto/rand unavailable: " + err.Error())
	}
	return binary.LittleEndian.Uint64(b[:])
}

type lockedRand struct {
	mu sync.Mutex
	r  *mrand.Rand
}

func (l *lockedRand) Read(p []byte) (int, error) {
	l.mu.Lock()
	defer l.mu.Unlock()
	return l.r.Read(p)
}

var tokenRand = &lockedRand{r: mrand.New(cryptoSource{})}
