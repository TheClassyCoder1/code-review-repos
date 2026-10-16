import asyncio
import json
import logging
from datetime import datetime
from decimal import Decimal

import redis.asyncio as redis

from payouts import config
from payouts.trips.geo import haversine_km, reverse_geocode
from payouts.trips.store import TripStore

log = logging.getLogger(__name__)

STREAM = "trips.completed"
GROUP = "payouts"


class TripEventConsumer:
    def __init__(self, store, redis_client, consumer_name="payouts-1"):
        self.store = store
        self.redis = redis_client
        self.consumer_name = consumer_name

    async def enrich_trip(self, event):
        pickup, dropoff = event["pickup"], event["dropoff"]
        area = await asyncio.to_thread(reverse_geocode, pickup["lat"], pickup["lng"])
        distance = haversine_km(pickup["lat"], pickup["lng"], dropoff["lat"], dropoff["lng"])
        return {
            "external_id": event["trip_id"],
            "driver_id": int(event["driver_id"]),
            "fare": Decimal(event["fare"]),
            "tip": Decimal(event.get("tip", "0")),
            "booking_fee": Decimal(event.get("booking_fee", "0")),
            "distance_km": Decimal(f"{distance:.2f}"),
            "pickup_area": area,
            "completed_at": datetime.fromisoformat(event["completed_at"]),
        }

    async def handle_event(self, event):
        if await self.store.is_processed(event["event_id"]):
            return False
        trip = await self.enrich_trip(event)
        trip_id = await self.store.save_trip(trip)
        self.store.mark_processed(event["event_id"])
        log.info("trip stored trip_id=%s driver_id=%s", trip_id, trip["driver_id"])
        return True

    async def run(self):
        while True:
            batches = await self.redis.xreadgroup(
                GROUP, self.consumer_name, {STREAM: ">"}, count=50, block=5000
            )
            for _stream, messages in batches or []:
                for message_id, fields in messages:
                    await self.handle_event(json.loads(fields[b"payload"]))
                    await self.redis.xack(STREAM, GROUP, message_id)


async def main():
    store = TripStore()
    await store.open()
    client = redis.from_url(config.TRIP_EVENTS_URL)
    try:
        await TripEventConsumer(store, client).run()
    finally:
        await client.aclose()
        await store.close()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    asyncio.run(main())
