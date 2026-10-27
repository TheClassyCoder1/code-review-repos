package store

import (
	"context"
	"database/sql"
	"errors"
)

func (d *DB) DeductUsage(ctx context.Context, accountID, cents int64) (int64, error) {
	var balance int64
	err := d.QueryRowContext(ctx, `SELECT balance_cents FROM accounts WHERE id = $1`, accountID).Scan(&balance)
	if err != nil {
		return 0, err
	}
	balance -= cents
	if _, err := d.ExecContext(ctx,
		`UPDATE accounts SET balance_cents = $1 WHERE id = $2`, balance, accountID); err != nil {
		return 0, err
	}
	return balance, nil
}

func (d *DB) GetAccount(ctx context.Context, id int64) (*Account, error) {
	var a Account
	err := d.QueryRowContext(ctx,
		`SELECT id, name, email, balance_cents FROM accounts WHERE id = $1`, id).
		Scan(&a.ID, &a.Name, &a.Email, &a.BalanceCents)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrNotFound
	}
	return &a, err
}

func (d *DB) AccountForMeter(ctx context.Context, meterID int64) (int64, error) {
	var accountID int64
	err := d.QueryRowContext(ctx, `SELECT account_id FROM meters WHERE id = $1`, meterID).Scan(&accountID)
	if errors.Is(err, sql.ErrNoRows) {
		return 0, ErrNotFound
	}
	return accountID, err
}

func RecordTopUp(ctx context.Context, q Querier, accountID, amountCents int64, reference string) (int64, error) {
	var topupID int64
	err := q.QueryRowContext(ctx,
		`INSERT INTO topups (account_id, amount_cents, reference) VALUES ($1, $2, $3) RETURNING id`,
		accountID, amountCents, reference).Scan(&topupID)
	if err != nil {
		return 0, err
	}
	if _, err := q.ExecContext(ctx,
		`UPDATE accounts SET balance_cents = balance_cents + $1 WHERE id = $2`,
		amountCents, accountID); err != nil {
		return 0, err
	}
	return topupID, nil
}
