# opsdesk API image.
# Stage 1 compiles wheels (needs a compiler toolchain and root for apt);
# stage 2 is the slim runtime and runs unprivileged.

FROM python:3.12.7-slim-bookworm AS build
USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential libpq-dev \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY requirements.txt .
RUN pip wheel --no-cache-dir --wheel-dir /wheels -r requirements.txt

FROM python:3.12.7-slim-bookworm AS runtime
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1
RUN apt-get update \
    && apt-get install -y --no-install-recommends libpq5 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 opsdesk \
    && useradd --system --uid 10001 --gid opsdesk --no-create-home --shell /usr/sbin/nologin opsdesk
WORKDIR /app
COPY --from=build /wheels /wheels
RUN pip install --no-cache-dir /wheels/* && rm -rf /wheels
COPY --chown=opsdesk:opsdesk opsdesk ./opsdesk
COPY --chown=opsdesk:opsdesk tasks.py ./
USER 10001:10001
EXPOSE 8120
CMD ["uvicorn", "opsdesk.main:app", "--host", "0.0.0.0", "--port", "8120", "--proxy-headers"]
