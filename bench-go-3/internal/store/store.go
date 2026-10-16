package store

import (
	"database/sql"
	"errors"

	"github.com/jmoiron/sqlx"
	_ "github.com/lib/pq"
)

var ErrNotFound = errors.New("not found")

type Store struct {
	db *sqlx.DB
}

func Open(dsn string) (*Store, error) {
	db, err := sqlx.Connect("postgres", dsn)
	if err != nil {
		return nil, err
	}
	db.SetMaxOpenConns(20)
	return &Store{db: db}, nil
}

func notFound(err error) error {
	if errors.Is(err, sql.ErrNoRows) {
		return ErrNotFound
	}
	return err
}
