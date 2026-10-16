from contextlib import contextmanager

from psycopg.rows import dict_row
from psycopg_pool import ConnectionPool

from payouts import config

pool = ConnectionPool(
    config.DATABASE_URL,
    min_size=2,
    max_size=20,
    kwargs={"row_factory": dict_row},
    open=False,
)


def open_pool():
    pool.open(wait=True, timeout=30)


def close_pool():
    pool.close()


@contextmanager
def transaction():
    with pool.connection() as conn:
        with conn.transaction():
            yield conn


def release(conn):
    pool.putconn(conn)


def pool_stats():
    stats = pool.get_stats()
    return {
        "size": stats.get("pool_size", 0),
        "available": stats.get("pool_available", 0),
        "waiting": stats.get("requests_waiting", 0),
    }


def checkout():
    conn = pool.getconn()
    conn.autocommit = False
    return conn
