package httpx

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"net/http"
	"strings"

	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/store"
)

type advertiserKey struct{}

// CampaignKey is the context key under which LoadCampaign stores the campaign.
type CampaignKey struct{}

// AdminDigestKey is the context key holding the SHA-256 hex digest of the
// presented admin key.
type AdminDigestKey struct{}

// AdvertiserID returns the authenticated advertiser id from ctx.
func AdvertiserID(ctx context.Context) int64 {
	id, _ := ctx.Value(advertiserKey{}).(int64)
	return id
}

// RequireAdvertiser authenticates the request by its API key and stores the
// advertiser id in the request context.
func RequireAdvertiser(st *store.Store, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		key, ok := strings.CutPrefix(r.Header.Get("Authorization"), "Bearer ")
		if !ok || key == "" {
			WriteError(w, http.StatusUnauthorized, "missing API key")
			return
		}
		sum := sha256.Sum256([]byte(key))
		id, err := st.AdvertiserByKeyHash(r.Context(), hex.EncodeToString(sum[:]))
		if errors.Is(err, store.ErrNotFound) {
			WriteError(w, http.StatusUnauthorized, "invalid API key")
			return
		}
		if err != nil {
			WriteError(w, http.StatusInternalServerError, "authentication failed")
			return
		}
		ctx := context.WithValue(r.Context(), advertiserKey{}, id)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// LoadCampaign resolves the {id} path parameter to a campaign owned by the
// authenticated advertiser and stores it under CampaignKey.
func LoadCampaign(st *store.Store, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id, ok := PathID(r, "id")
		if !ok {
			WriteError(w, http.StatusBadRequest, "invalid campaign id")
			return
		}
		c, err := st.GetCampaign(r.Context(), id)
		if err != nil && !errors.Is(err, store.ErrNotFound) {
			WriteError(w, http.StatusInternalServerError, "campaign lookup failed")
			return
		}
		if c == nil || c.AdvertiserID != AdvertiserID(r.Context()) {
			WriteError(w, http.StatusNotFound, "campaign not found")
			return
		}
		ctx := context.WithValue(r.Context(), CampaignKey{}, c)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// AdminDigest hashes the X-Admin-Key header and stores the hex digest under
// AdminDigestKey for admin handlers.
func AdminDigest(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		sum := sha256.Sum256([]byte(r.Header.Get("X-Admin-Key")))
		ctx := context.WithValue(r.Context(), AdminDigestKey{}, hex.EncodeToString(sum[:]))
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
