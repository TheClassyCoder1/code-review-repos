import hmac

from flask import Flask, abort, g, jsonify, request
from itsdangerous import BadSignature, SignatureExpired, URLSafeTimedSerializer

from payouts import config, db


def verify_driver_token(token):
    serializer = URLSafeTimedSerializer(config.SECRET_KEY, salt="driver-session")
    try:
        data = serializer.loads(token, max_age=config.DRIVER_TOKEN_MAX_AGE_SECONDS)
    except (BadSignature, SignatureExpired):
        return None
    return data.get("driver_id")


def authenticate():
    if request.path == "/health" or request.path.startswith("/webhooks/"):
        return None
    if request.path.startswith("/ops/"):
        supplied = request.headers.get("X-Ops-Key", "")
        if not hmac.compare_digest(supplied.encode(), config.OPS_API_KEY.encode()):
            abort(401)
        return None
    token = request.headers.get("Authorization", "").removeprefix("Bearer ").strip()
    driver_id = verify_driver_token(token) if token else None
    if driver_id is None:
        abort(401)
    with db.pool.connection() as conn:
        row = conn.execute(
            "SELECT id, status FROM drivers WHERE id = %s", (driver_id,)
        ).fetchone()
    if row is None or row["status"] != "active":
        abort(401)
    g.driver_id = row["id"]
    return None


def create_app():
    from payouts.drivers.routes import drivers_bp
    from payouts.settlements.routes import ops_bp
    from payouts.webhooks import webhooks_bp

    app = Flask(__name__)
    app.config.from_object(config)
    db.open_pool()
    app.before_request(authenticate)
    app.register_blueprint(drivers_bp)
    app.register_blueprint(ops_bp)
    app.register_blueprint(webhooks_bp)

    @app.get("/health")
    def health():
        return jsonify(status="ok", db=db.pool_stats())

    return app


app = create_app()
