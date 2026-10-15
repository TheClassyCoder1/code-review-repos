package store

import (
	"context"
	"fmt"

	"artifactd/internal/models"
)

const repoColumns = "id, namespace, name, description, visibility, created_at"

func (s *Store) GetRepository(ctx context.Context, id int64) (*models.Repository, error) {
	var repo models.Repository
	err := s.db.GetContext(ctx, &repo, "SELECT "+repoColumns+" FROM repositories WHERE id = $1", id)
	if err != nil {
		return nil, notFound(err)
	}
	return &repo, nil
}

func (s *Store) ListRepositories(ctx context.Context, namespace string, afterID int64, limit int) ([]models.Repository, error) {
	repos := []models.Repository{}
	err := s.db.SelectContext(ctx, &repos,
		"SELECT "+repoColumns+" FROM repositories WHERE namespace = $1 AND id > $2 ORDER BY id LIMIT $3",
		namespace, afterID, limit)
	return repos, err
}

func (s *Store) CreateRepository(ctx context.Context, repo *models.Repository) error {
	tx, err := s.db.BeginTxx(ctx, nil)
	if err != nil {
		return err
	}
	defer tx.Rollback()
	err = tx.QueryRowxContext(ctx,
		`INSERT INTO repositories (namespace, name, description, visibility)
		 VALUES ($1, $2, $3, $4) RETURNING id, created_at`,
		repo.Namespace, repo.Name, repo.Description, repo.Visibility).Scan(&repo.ID, &repo.CreatedAt)
	if err != nil {
		return err
	}
	if _, err = tx.ExecContext(ctx,
		`UPDATE namespaces SET repository_count = repository_count + 1 WHERE name = $1`, repo.Namespace); err != nil {
		return err
	}
	return tx.Commit()
}

func (s *Store) DeleteRepository(ctx context.Context, id int64) error {
	_, err := s.db.ExecContext(ctx, `DELETE FROM repositories WHERE id = $1`, id)
	return err
}

func (s *Store) SearchRepositories(ctx context.Context, namespace, query string, limit int) ([]models.Repository, error) {
	repos := []models.Repository{}
	q := fmt.Sprintf(`SELECT %s FROM repositories
		WHERE namespace = $1 AND (name ILIKE '%%%s%%' OR description ILIKE '%%%s%%')
		ORDER BY name LIMIT $2`, repoColumns, query, query)
	if err := s.db.SelectContext(ctx, &repos, q, namespace, limit); err != nil {
		return nil, err
	}
	return repos, nil
}

func (s *Store) ListTags(ctx context.Context, repoID int64) ([]string, error) {
	tags := []string{}
	err := s.db.SelectContext(ctx, &tags,
		`SELECT tag FROM artifacts WHERE repository_id = $1 ORDER BY tag`, repoID)
	return tags, err
}

func (s *Store) ListArtifacts(ctx context.Context, repoID int64, orderBy string) ([]models.Artifact, error) {
	artifacts := []models.Artifact{}
	q := `SELECT id, repository_id, tag, digest, media_type, size_bytes, pushed_at
		FROM artifacts WHERE repository_id = $1 ORDER BY ` + orderBy
	if err := s.db.SelectContext(ctx, &artifacts, q, repoID); err != nil {
		return nil, err
	}
	return artifacts, nil
}

func (s *Store) UpsertArtifact(ctx context.Context, a *models.Artifact) error {
	return s.db.QueryRowxContext(ctx,
		`INSERT INTO artifacts (repository_id, tag, digest, media_type, size_bytes)
		 VALUES ($1, $2, $3, $4, $5)
		 ON CONFLICT (repository_id, tag) DO UPDATE
		   SET digest = EXCLUDED.digest, media_type = EXCLUDED.media_type,
		       size_bytes = EXCLUDED.size_bytes, pushed_at = now()
		 RETURNING id, pushed_at`,
		a.RepositoryID, a.Tag, a.Digest, a.MediaType, a.SizeBytes).Scan(&a.ID, &a.PushedAt)
}

func (s *Store) RecordBlob(ctx context.Context, repoID int64, digest string, size int64) error {
	_, err := s.db.ExecContext(ctx,
		`INSERT INTO repository_blobs (repository_id, digest, size_bytes) VALUES ($1, $2, $3)
		 ON CONFLICT (repository_id, digest) DO NOTHING`, repoID, digest, size)
	return err
}
