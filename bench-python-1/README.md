# School fees service

Flask service for the bursar's office and the guardian portal: students, guardians,
fee invoices, card payments and academic transcripts.

```bash
pip install -r requirements.txt
flask --app school_fees:create_app init-db
flask --app school_fees:create_app run
```

Configuration comes from environment variables (see `school_fees/config.py`).
