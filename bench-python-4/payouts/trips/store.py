from psycopg.rows import dict_row
from psycopg_pool import AsyncConnectionPool

from payouts import config
from payouts.earnings.calculator import trip_net_earning


class TripStore:
    def __init__(self, dsn=config.DATABASE_URL):
        self.pool = AsyncConnectionPool(
            dsn, min_size=1, max_size=5, kwargs={"row_factory": dict_row}, open=False
        )

    async def open(self):
        await self.pool.open(wait=True, timeout=30)

    async def close(self):
        await self.pool.close()

    async def is_processed(self, event_id):
        async with self.pool.connection() as conn:
            cur = await conn.execute(
                "SELECT 1 FROM processed_events WHERE event_id = %s",
                (event_id,),
            )
            return await cur.fetchone() is not None

    async def mark_processed(self, event_id):
        async with self.pool.connection() as conn:
            await conn.execute(
                "INSERT INTO processed_events (event_id) VALUES (%s) ON CONFLICT DO NOTHING",
                (event_id,),
            )

    async def save_trip(self, trip):
        async with self.pool.connection() as conn:
            async with conn.transaction():
                cur = await conn.execute(
                    "INSERT INTO trips (external_id, driver_id, fare, tip, booking_fee, "
                    "distance_km, pickup_area, completed_at) "
                    "VALUES (%(external_id)s, %(driver_id)s, %(fare)s, %(tip)s, %(booking_fee)s, "
                    "%(distance_km)s, %(pickup_area)s, %(completed_at)s) RETURNING id",
                    trip,
                )
                trip_id = (await cur.fetchone())["id"]
                await conn.execute(
                    "INSERT INTO earnings (trip_id, driver_id, amount, earned_at) "
                    "VALUES (%s, %s, %s, %s)",
                    (trip_id, trip["driver_id"], trip_net_earning(trip), trip["completed_at"]),
                )
                return trip_id
