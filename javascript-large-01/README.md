# statement-export-service

Node/Express service that renders and serves loan statements, exports partner
documents, and ingests partner statement feeds for the lending platform.

It sits behind the portal BFF and talks to the archive tier for long-term
document storage. It is intentionally small and stateless: all persistent state
lives in the archive tier and the primary datastore.

## Endpoints (all under `/api/v1`)

| Method | Path | Purpose |
|--------|------|---------|
| GET  | `/statements/:id` | fetch a rendered statement by id |
| GET  | `/statements/:id/file` | stream the statement PDF |
| GET  | `/documents` | download a stored document |
| POST | `/documents/convert` | convert an uploaded document to PDF |
| POST | `/reports/export` | export an ad-hoc report |
| POST | `/imports/partner` | ingest a partner statement feed (XML/YAML) |
| POST | `/webhooks/subscribe` | register a delivery callback |
| POST | `/account/email` | update the statement delivery address |

## Configuration

See `.env.example`. Secrets are read from the environment at boot; the service
refuses to start in a non-development environment when a required secret is
missing (see `src/config`).

## Running

```
npm install
npm start
```
