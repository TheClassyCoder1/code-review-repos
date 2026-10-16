import { z } from 'zod';
import { pool } from '../db';

export const createCustomerSchema = z.object({
  email: z.string().email().max(254),
  name: z.string().min(1).max(200),
});

export interface Customer {
  id: string;
  account_id: string;
  email: string;
  name: string;
}

export async function createCustomer(accountId: string, input: unknown): Promise<Customer> {
  const parsed = createCustomerSchema.parse(input);
  const { rows } = await pool.query<Customer>(
    `INSERT INTO customers (account_id, email, name)
     VALUES ($1, lower($2), $3)
     RETURNING id, account_id, email, name`,
    [accountId, parsed.email, parsed.name],
  );
  return rows[0];
}
