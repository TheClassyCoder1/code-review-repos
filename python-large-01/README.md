# opsdesk

Back-office operations service for the lending platform. `opsdesk.main:app`
serves the FastAPI admin API (`opsdesk/api`) and mounts the Flask reporting
console (`/console`) and the Django back-office (`/backoffice`). Run with
`uvicorn opsdesk.main:app --port 8120`; all configuration comes from the
environment (`opsdesk/settings.py`).
