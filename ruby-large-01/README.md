# document-intake

Sinatra service that receives borrower documents for loan applications
(bank statements, payslips, ID scans), extracts text, scans uploads, pulls
bureau reports and archives completed packs for loan-service.

## Layout

| Path | Purpose |
|------|---------|
| `config/boot.rb` | Load path, logger, database connection |
| `config/settings.rb` | Environment-driven configuration and boot-time checks |
| `app/api.rb` | JSON API mounted at `/api/v1` (used by loan-service and portal-bff) |
| `app/portal.rb` | Borrower-facing HTML portal mounted at `/portal` |
| `lib/doc_intake/repositories` | Sequel-backed persistence |
| `lib/doc_intake/services` | Parsing, scanning, archiving and outbound integrations |
| `lib/doc_intake/support` | Logging, errors, crypto helpers |

## Running locally

```sh
bundle install
DATABASE_URL=sqlite://tmp/intake.db bundle exec puma config.ru -p 8120
```

## Configuration

All settings come from environment variables; see `config/settings.rb`.
Secrets are injected by the deployment and are never committed.
