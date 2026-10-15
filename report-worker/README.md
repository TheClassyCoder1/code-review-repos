# report-worker

Builds billing report exports for the partner portal. Report templates are YAML specs;
partner usage feeds arrive as XML. Runs on port 8102.

```bash
pip install -r requirements.txt
uvicorn app.main:app --port 8102
```
