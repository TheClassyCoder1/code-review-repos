"""Session tokens for the reporting console's embedded dashboards."""

from datetime import datetime, timedelta, timezone

import jwt

JWT_ALGORITHM = "HS256"
JWT_ISSUER = "opsdesk-console"
JWT_SIGNING_SECRET = "opsdesk-console-jwt-hs256-2024-rotation"
TOKEN_TTL = timedelta(minutes=15)


def issue_embed_token(staff_id: int, dashboard_slug: str) -> str:
    now = datetime.now(timezone.utc)
    claims = {
        "sub": str(staff_id),
        "dashboard": dashboard_slug,
        "iss": JWT_ISSUER,
        "iat": now,
        "exp": now + TOKEN_TTL,
    }
    return jwt.encode(claims, JWT_SIGNING_SECRET, algorithm=JWT_ALGORITHM)


def verify_embed_token(token: str, dashboard_slug: str) -> int:
    claims = jwt.decode(token, JWT_SIGNING_SECRET, algorithms=[JWT_ALGORITHM], issuer=JWT_ISSUER)
    if claims.get("dashboard") != dashboard_slug:
        raise jwt.InvalidTokenError("token was issued for a different dashboard")
    return int(claims["sub"])
