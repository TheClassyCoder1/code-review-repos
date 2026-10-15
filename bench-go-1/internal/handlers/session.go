package handlers

import (
	"bytes"
	"encoding/base64"
	"encoding/gob"
	"net/http"
	"time"

	"fleettrack/internal/auth"
)

const sessionTTL = 12 * time.Hour

func (h *Handler) Login(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseForm(); err != nil {
		http.Error(w, "bad request", http.StatusBadRequest)
		return
	}
	acct, err := h.db.FindAccountByEmail(r.Context(), r.PostFormValue("email"))
	if err != nil || !auth.CheckPassword(acct.PasswordHash, r.PostFormValue("password")) {
		http.Error(w, "invalid credentials", http.StatusUnauthorized)
		return
	}
	u := auth.User{ID: acct.ID, FleetID: acct.FleetID, Role: acct.Role}
	http.SetCookie(w, &http.Cookie{
		Name:     auth.SessionCookie,
		Value:    h.sessions.Sign(auth.SessionPayload(u, time.Now().Add(sessionTTL))),
		Path:     "/",
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteLaxMode,
	})
	next := r.URL.Query().Get("next")
	if next == "" {
		next = "/dashboard"
	}
	http.Redirect(w, r, next, http.StatusSeeOther)
}

const dispatchCookie = "dispatch_state"

func clearCookie(name string) *http.Cookie {
	return &http.Cookie{Name: name, Value: "", Path: "/", MaxAge: -1, HttpOnly: true, Secure: true}
}

func (h *Handler) Logout(w http.ResponseWriter, r *http.Request) {
	http.SetCookie(w, clearCookie(auth.SessionCookie))
	http.SetCookie(w, clearCookie(dispatchCookie))
	target := r.URL.Query().Get("next")
	if target == "" {
		target = "/login"
	}
	http.Redirect(w, r, target, http.StatusSeeOther)
}

type dispatchSession struct {
	DriverID int64
	FleetID  int64
	Role     string
	Shift    string
}

func loadDispatchSession(r *http.Request) (dispatchSession, error) {
	c, err := r.Cookie(dispatchCookie)
	if err != nil {
		return dispatchSession{}, err
	}
	raw, err := base64.URLEncoding.DecodeString(c.Value)
	if err != nil {
		return dispatchSession{}, err
	}
	var s dispatchSession
	if err := gob.NewDecoder(bytes.NewReader(raw)).Decode(&s); err != nil {
		return dispatchSession{}, err
	}
	return s, nil
}

func (h *Handler) DispatchBoard(w http.ResponseWriter, r *http.Request) {
	s, err := loadDispatchSession(r)
	if err != nil {
		http.Redirect(w, r, "/login?next=/dispatch", http.StatusSeeOther)
		return
	}
	if s.Role != "dispatcher" && s.Role != "admin" {
		http.Error(w, "forbidden", http.StatusForbidden)
		return
	}
	trips, err := h.db.ListActiveTrips(r.Context(), s.FleetID)
	if err != nil {
		storeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, trips)
}
