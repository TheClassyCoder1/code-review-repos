package store

import (
	"context"
	"database/sql"
	"errors"
	"time"
)

func (d *DB) IssueBill(ctx context.Context, b *Bill, lines []BillLine) error {
	err := d.QueryRowContext(ctx,
		`INSERT INTO bills (meter_id, account_id, period_start, period_end, energy_wh, amount_cents, status)
		 VALUES ($1, $2, $3, $4, $5, $6, 'issued') RETURNING id`,
		b.MeterID, b.AccountID, b.PeriodStart, b.PeriodEnd, b.EnergyWh, b.AmountCents).Scan(&b.ID)
	if err != nil {
		return err
	}
	for _, l := range lines {
		if _, err := d.ExecContext(ctx,
			`INSERT INTO bill_lines (bill_id, description, amount_cents) VALUES ($1, $2, $3)`,
			b.ID, l.Description, l.AmountCents); err != nil {
			return err
		}
	}
	_, err = d.ExecContext(ctx, `UPDATE meters SET billed_until = $1 WHERE id = $2`, b.PeriodEnd, b.MeterID)
	if err != nil {
		return err
	}
	b.Status = "issued"
	return nil
}

func (d *DB) MarkBillsIssued(ctx context.Context, periodEnd time.Time) (int64, error) {
	res, err := d.ExecContext(ctx,
		`UPDATE bills SET status = 'issued' WHERE status = 'draft' AND period_end <= $1`, periodEnd)
	if err != nil {
		return 0, err
	}
	return res.RowsAffected()
}

func (d *DB) GetBillWithCustomer(ctx context.Context, id int64) (*Bill, []BillLine, error) {
	var b Bill
	var c Account
	err := d.QueryRowContext(ctx,
		`SELECT b.id, b.meter_id, b.account_id, b.period_start, b.period_end, b.energy_wh, b.amount_cents, b.status,
		        a.id, a.name, a.email, a.balance_cents
		 FROM bills b JOIN accounts a ON a.id = b.account_id WHERE b.id = $1`, id).
		Scan(&b.ID, &b.MeterID, &b.AccountID, &b.PeriodStart, &b.PeriodEnd, &b.EnergyWh, &b.AmountCents, &b.Status,
			&c.ID, &c.Name, &c.Email, &c.BalanceCents)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, nil, ErrNotFound
	}
	if err != nil {
		return nil, nil, err
	}
	b.Customer = &c
	rows, err := d.QueryContext(ctx,
		`SELECT description, amount_cents FROM bill_lines WHERE bill_id = $1 ORDER BY description`, id)
	if err != nil {
		return nil, nil, err
	}
	defer rows.Close()
	var lines []BillLine
	for rows.Next() {
		var l BillLine
		if err := rows.Scan(&l.Description, &l.AmountCents); err != nil {
			return nil, nil, err
		}
		lines = append(lines, l)
	}
	return &b, lines, rows.Err()
}

func (d *DB) ListBillsForAccount(ctx context.Context, accountID int64, limit int) ([]Bill, error) {
	rows, err := d.QueryContext(ctx,
		`SELECT id, meter_id, account_id, period_start, period_end, energy_wh, amount_cents, status
		 FROM bills WHERE account_id = $1 ORDER BY period_end DESC LIMIT $2`, accountID, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	bills := []Bill{}
	for rows.Next() {
		var b Bill
		if err := rows.Scan(&b.ID, &b.MeterID, &b.AccountID, &b.PeriodStart, &b.PeriodEnd,
			&b.EnergyWh, &b.AmountCents, &b.Status); err != nil {
			return nil, err
		}
		bills = append(bills, b)
	}
	return bills, rows.Err()
}

func AdjustBillAmount(ctx context.Context, q Querier, billID, deltaCents int64) (int64, error) {
	var amount int64
	err := q.QueryRowContext(ctx, `SELECT amount_cents FROM bills WHERE id = $1`, billID).Scan(&amount)
	if errors.Is(err, sql.ErrNoRows) {
		return 0, ErrNotFound
	}
	if err != nil {
		return 0, err
	}
	amount += deltaCents
	if _, err := q.ExecContext(ctx,
		`UPDATE bills SET amount_cents = $1 WHERE id = $2`, amount, billID); err != nil {
		return 0, err
	}
	return amount, nil
}
