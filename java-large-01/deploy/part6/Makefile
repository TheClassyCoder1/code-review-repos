# Run from java-large-01/: make -f deploy/part6/Makefile <target>
REGISTRY ?= registry.lending.internal/ops
GIT_SHA  := $(shell git rev-parse --short=12 HEAD)

.PHONY: dev-up dev-down release

# Local stack only; the compose file is never used by CI or the cluster.
dev-up:
	docker compose -f deploy/part6/docker-compose.dev.yml up -d --build

dev-down:
	docker compose -f deploy/part6/docker-compose.dev.yml down -v

# Release images are always tagged with the commit SHA, never a floating tag.
release:
	docker build -f deploy/part6/Dockerfile -t $(REGISTRY)/ops-console:$(GIT_SHA) .
	docker build -f deploy/part6/outreach-worker.Dockerfile -t $(REGISTRY)/outreach-worker:$(GIT_SHA) .
	docker push $(REGISTRY)/ops-console:$(GIT_SHA)
	docker push $(REGISTRY)/outreach-worker:$(GIT_SHA)
