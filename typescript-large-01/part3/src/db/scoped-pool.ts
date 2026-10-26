import { AsyncLocalStorage } from 'async_hooks';

export interface Connection {
  readonly id: number;
  query<T>(sql: string, params?: unknown[]): Promise<T[]>;
  execute(sql: string, params?: unknown[]): Promise<number>;
}

export interface ConnectionFactory {
  open(): Promise<Connection>;
}

/**
 * Connection pool whose leases are tied to a unit of work. Code running inside
 * runScope() can acquire() freely: every connection acquired in the scope is
 * returned to the pool when the scope ends, whether the work succeeded or threw.
 * acquire() outside a scope is rejected, so a lease can never outlive its job.
 */
export class ScopedPool {
  private readonly idle: Connection[] = [];
  private readonly waiters: Array<(c: Connection) => void> = [];
  private readonly scopes = new AsyncLocalStorage<Connection[]>();
  private open = 0;

  constructor(
    private readonly factory: ConnectionFactory,
    private readonly size: number,
  ) {}

  async runScope<T>(name: string, work: () => Promise<T>): Promise<T> {
    const acquired: Connection[] = [];
    try {
      return await this.scopes.run(acquired, work);
    } finally {
      for (const conn of acquired.splice(0)) {
        this.release(conn);
      }
    }
  }

  async acquire(): Promise<Connection> {
    const acquired = this.scopes.getStore();
    if (!acquired) {
      throw new Error('ScopedPool.acquire() called outside runScope()');
    }
    const conn = await this.lease();
    acquired.push(conn);
    return conn;
  }

  private async lease(): Promise<Connection> {
    const idle = this.idle.pop();
    if (idle) {
      return idle;
    }
    if (this.open < this.size) {
      this.open++;
      try {
        return await this.factory.open();
      } catch (err) {
        this.open--;
        throw err;
      }
    }
    return new Promise((resolve) => this.waiters.push(resolve));
  }

  private release(conn: Connection): void {
    const waiter = this.waiters.shift();
    if (waiter) {
      waiter(conn);
    } else {
      this.idle.push(conn);
    }
  }
}
