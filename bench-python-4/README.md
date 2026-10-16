# Driver payouts service

Tracks completed trips, driver earnings and weekly settlements for the ride-hailing
platform, and pays drivers out through the partner bank.

- `payouts/app.py` - Flask API (driver endpoints, ops endpoints, bank webhooks)
- `payouts/trips/consumer.py` - asyncio consumer for trip-completed events
- `payouts/settlements/tasks.py` - Celery tasks (weekly settlement, payout dispatch, reconciliation)
- `schema.sql` - Postgres schema

Run locally:

```bash
pip install -r requirements.txt
flask --app payouts.app run
celery -A payouts.celery_app worker -B
python -m payouts.trips.consumer
```
