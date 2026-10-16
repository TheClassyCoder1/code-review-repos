package handlers

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"io"
	"net/http"
	"time"

	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/httpx"
	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/store"
)

// ReceiveConversion handles POST /webhooks/conversions from advertiser
// tracking pixels. The body is signed with the shared conversion secret.
func (a *API) ReceiveConversion(w http.ResponseWriter, r *http.Request) {
	body, err := io.ReadAll(r.Body)
	if err != nil {
		httpx.WriteError(w, http.StatusBadRequest, "could not read body")
		return
	}
	if !verifySignature(a.Cfg.ConversionSecret, body, r.Header.Get(signatureHeader)) {
		httpx.WriteError(w, http.StatusUnauthorized, "bad signature")
		return
	}
	var conv store.Conversion
	if err := json.Unmarshal(body, &conv); err != nil || conv.ImpressionID == "" {
		httpx.WriteError(w, http.StatusBadRequest, "impression_id is required")
		return
	}
	if conv.OccurredAt.IsZero() {
		conv.OccurredAt = time.Now().UTC()
	}
	if err := a.Store.RecordConversion(r.Context(), conv); err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "could not record conversion")
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// signatureHeader is the request header carrying the hex HMAC-SHA256 of the
// raw request body.
const signatureHeader = "X-Signature"

// ConversionRoute is where advertisers' pixels post conversions.
const ConversionRoute = "POST /webhooks/conversions"

// verifySignature checks the hex HMAC-SHA256 signature of body.
func verifySignature(secret string, body []byte, signature string) bool {
	if secret == "" || signature == "" {
		return false
	}
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write(body)
	expected := hex.EncodeToString(mac.Sum(nil))
	return expected == signature
}
