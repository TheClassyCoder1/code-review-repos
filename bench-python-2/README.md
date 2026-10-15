# Loyalty points service

FastAPI + async SQLAlchemy service for the member loyalty programme: enrolment,
earn and burn rules, partner transaction feeds, reward redemptions and monthly
statements.

```bash
pip install -r requirements.txt
LOYALTY_DATABASE_URL=postgresql+asyncpg://loyalty@localhost/loyalty uvicorn loyalty.main:app
```
