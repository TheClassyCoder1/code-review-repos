# opsdesk background worker: identity-review reminders and analytics retries.

FROM python:3.12.7-slim-bookworm AS build
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential libpq-dev \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY requirements.txt .
RUN pip wheel --no-cache-dir --wheel-dir /wheels -r requirements.txt

FROM python:3.12.7-slim-bookworm
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1
RUN apt-get update \
    && apt-get install -y --no-install-recommends libpq5 tini \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /wheels /wheels
RUN pip install --no-cache-dir /wheels/* && rm -rf /wheels
COPY opsdesk ./opsdesk
COPY tasks.py ./
ENTRYPOINT ["/usr/bin/tini", "--"]
CMD ["invoke", "worker"]
