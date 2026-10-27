package auth

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"
)

type User struct {
	ID      int64
	FleetID int64
	Role    string
}

type userKey struct{}

func UserFrom(ctx context.Context) User {
	u, _ := ctx.Value(userKey{}).(User)
	return u
}

const SessionCookie = "fleet_session"

func SessionPayload(u User, expires time.Time) string {
	return fmt.Sprintf("%d:%d:%s:%d", u.ID, u.FleetID, u.Role, expires.Unix())
}

func parseSession(payload string, now time.Time) (User, error) {
	parts := strings.Split(payload, ":")
	if len(parts) != 4 {
		return User{}, errors.New("malformed session")
	}
	uid, err1 := strconv.ParseInt(parts[0], 10, 64)
	fid, err2 := strconv.ParseInt(parts[1], 10, 64)
	exp, err3 := strconv.ParseInt(parts[3], 10, 64)
	if err := errors.Join(err1, err2, err3); err != nil {
		return User{}, err
	}
	if now.Unix() >= exp {
		return User{}, errors.New("session expired")
	}
	return User{ID: uid, FleetID: fid, Role: parts[2]}, nil
}

func Authenticate(signer *Signer, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		raw, ok := strings.CutPrefix(r.Header.Get("Authorization"), "Bearer ")
		if !ok {
			c, err := r.Cookie(SessionCookie)
			if err != nil {
				http.Error(w, "unauthorized", http.StatusUnauthorized)
				return
			}
			raw = c.Value
		}
		payload, ok := signer.Verify(raw)
		if !ok {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		u, err := parseSession(payload, time.Now())
		if err != nil {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), userKey{}, u)))
	})
}

func RequireRole(role string, next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if UserFrom(r.Context()).Role != role {
			http.Error(w, "forbidden", http.StatusForbidden)
			return
		}
		next(w, r)
	}
}

func RequireVehicleAccess(fleetOf func(context.Context, int64) (int64, error), next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
		if err != nil {
			http.Error(w, "invalid vehicle id", http.StatusBadRequest)
			return
		}
		fleetID, err := fleetOf(r.Context(), id)
		if err != nil || fleetID != UserFrom(r.Context()).FleetID {
			http.Error(w, "not found", http.StatusNotFound)
			return
		}
		next(w, r)
	}
}

func isLocalPath(p string) bool {
	return strings.HasPrefix(p, "/") && !strings.HasPrefix(p, "//") && !strings.HasPrefix(p, "/\\")
}

func SanitizeNext(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		q := r.URL.Query()
		if v := q.Get("next"); v != "" && !isLocalPath(v) {
			q.Set("next", "/")
			r.URL.RawQuery = q.Encode()
		}
		next.ServeHTTP(w, r)
	})
}
