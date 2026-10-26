"""Single-page routes for the reporting console's dashboard viewer."""

from functools import wraps

from flask import Blueprint, current_app, make_response, render_template, safe_join, send_file
from flask_login import login_required

routes = Blueprint("dashboards", __name__, template_folder="templates")


def org_scoped_rule(rule):
    if current_app and current_app.config.get("MULTI_ORG"):
        return "/<org_slug>{}".format(rule)
    return rule


def csp_allows_embeding(fn):
    """Drop frame-ancestors so the page can be embedded in the portal iframe."""

    @wraps(fn)
    def decorated(*args, **kwargs):
        response = make_response(fn(*args, **kwargs))
        response.headers.pop("X-Frame-Options", None)
        policy = response.headers.get("Content-Security-Policy", "")
        directives = [d for d in policy.split(";") if d.strip() and not d.strip().startswith("frame-ancestors")]
        if directives:
            response.headers["Content-Security-Policy"] = ";".join(directives)
        return response

    return decorated


def render_index():
    if current_app.config.get("MULTI_ORG"):
        response = render_template("multi_org.html", base_href="/")
    else:
        full_path = safe_join(current_app.static_folder, "index.html")
        response = send_file(full_path, max_age=0, conditional=True)
    return response


@routes.route(org_scoped_rule("/dashboard/<slug>"), methods=["GET"])
@login_required
@csp_allows_embeding
def dashboard(slug, org_slug=None):
    return render_index()
