import base64
import pickle
from decimal import Decimal

import yaml
from flask import current_app

MAX_BATCH_RECORDS = 500


def parse_import_batch(raw_body):
    batch = pickle.loads(base64.b64decode(raw_body))
    if len(batch.get("students", [])) > MAX_BATCH_RECORDS:
        raise ValueError("import batch too large")
    return batch


def normalise_fee_code(code):
    return code.strip().upper().replace(" ", "-")


def load_fee_schedule(document):
    schedule = yaml.load(document, Loader=current_app.config["YAML_LOADER"])
    fees = {}
    for item in schedule.get("fees", []):
        fees[normalise_fee_code(item["code"])] = Decimal(str(item["amount"]))
    return fees
