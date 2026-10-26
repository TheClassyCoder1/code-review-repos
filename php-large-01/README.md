# borrower-documents

Laravel service that owns borrower documents for the lending platform: statement
imports, uploads (virus scan, text extraction, rendering), archives to the records
SFTP host, and the portal preference / payout pages served behind `portal-bff`.

```
composer install && cp .env.example .env && php artisan serve --port=8110
```

Binaries expected on the PATH: `gs`, `pdftotext`, `pdftoppm`, `clamdscan`, `zip`.
