package store

import (
	"context"

	"artifactd/internal/models"
)

func (s *Store) InsertAccessToken(ctx context.Context, t *models.AccessToken) error {
	rows, err := s.db.NamedQueryContext(ctx,
		`INSERT INTO access_tokens (namespace, name, token_hash, scopes, expires_at)
		 VALUES (:namespace, :name, :token_hash, :scopes, :expires_at) RETURNING id`, t)
	if err != nil {
		return err
	}
	defer rows.Close()
	if rows.Next() {
		return rows.Scan(&t.ID)
	}
	return rows.Err()
}

func (s *Store) FindActiveToken(ctx context.Context, hash string) (*models.AccessToken, error) {
	var t models.AccessToken
	err := s.db.GetContext(ctx, &t,
		`SELECT id, namespace, name, token_hash, scopes, expires_at, revoked_at
		 FROM access_tokens WHERE token_hash = $1 AND revoked_at IS NULL`, hash)
	if err != nil {
		return nil, notFound(err)
	}
	return &t, nil
}

func (s *Store) RevokeAccessToken(ctx context.Context, namespace string, id int64) (bool, error) {
	res, err := s.db.ExecContext(ctx,
		`UPDATE access_tokens SET revoked_at = now()
		 WHERE id = $1 AND namespace = $2 AND revoked_at IS NULL`, id, namespace)
	if err != nil {
		return false, err
	}
	n, err := res.RowsAffected()
	if err != nil {
		return false, err
	}
	return n == 1, nil
}

func (s *Store) InsertWebhook(ctx context.Context, w *models.Webhook) error {
	return s.db.QueryRowxContext(ctx,
		`INSERT INTO webhooks (repository_id, url, secret, events) VALUES ($1, $2, $3, $4)
		 RETURNING id, created_at`, w.RepositoryID, w.URL, w.Secret, w.Events).Scan(&w.ID, &w.CreatedAt)
}

func (s *Store) ListWebhooks(ctx context.Context, repoID int64) ([]models.Webhook, error) {
	hooks := []models.Webhook{}
	err := s.db.SelectContext(ctx, &hooks,
		`SELECT id, repository_id, url, secret, events, created_at FROM webhooks WHERE repository_id = $1 ORDER BY id`, repoID)
	return hooks, err
}

func (s *Store) GetUserByEmail(ctx context.Context, email string) (*models.User, error) {
	var u models.User
	if err := s.db.GetContext(ctx, &u,
		`SELECT id, email, namespace, password_hash, salt FROM users WHERE lower(email) = lower($1)`, email); err != nil {
		return nil, notFound(err)
	}
	return &u, nil
}
