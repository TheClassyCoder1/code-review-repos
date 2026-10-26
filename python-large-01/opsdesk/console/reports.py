"""Saved report and report-pack endpoints."""

from flask import Blueprint, abort, session
from marshmallow import Schema, fields, validate

from opsdesk.console.query_runner import get_runner
from opsdesk.console.scaffold import make_boilerplate
from opsdesk.console.schema import validate_schema
from opsdesk.settings import settings

reports_blueprint = Blueprint("reports", __name__)
DATA_SOURCES = {"replica": ("pg_replica", {"url": "postgresql+psycopg://reporting@replica/lending"}), "scripts": ("script", {})}


class RunReportInput(Schema):
    data_source = fields.String(required=True, validate=validate.OneOf(sorted(DATA_SOURCES)))
    query = fields.String(required=True, validate=validate.Length(min=1, max=20000))


class NewPackInput(Schema):
    name = fields.String(required=True)
    files = fields.List(fields.String(), missing=None)


@reports_blueprint.before_request
def _require_login():
    if not session.get("user_id"):
        abort(401)


@reports_blueprint.route("/", endpoint="list_reports")
def list_reports():
    return {"data_sources": sorted(DATA_SOURCES)}


@reports_blueprint.route("/run", methods=["POST"])
@validate_schema(RunReportInput(), None)
def run_report(data=None):
    rows, error = get_runner(*DATA_SOURCES[data["data_source"]]).run_query(data["query"], session["user_id"])
    return (dict(message=error), 400) if error else dict(rows=rows, row_count=len(rows))


@reports_blueprint.route("/schema")
@validate_schema(None, None)
def replica_schema():
    return get_runner(*DATA_SOURCES["replica"]).get_schema(get_stats=True)


@reports_blueprint.route("/packs", methods=["POST"])
@validate_schema(NewPackInput(), None)
def create_pack(data=None):
    return dict(path=make_boilerplate(settings.deploy_root + "/report-packs", data["name"], data["files"])), 201
