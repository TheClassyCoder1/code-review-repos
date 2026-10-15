from starlette.types import ASGIApp, Message, Receive, Scope, Send

MAX_UPLOAD_BYTES = 2 * 1024 * 1024


class PayloadTooLarge(Exception):
    pass


class BodySizeLimitMiddleware:
    def __init__(self, app: ASGIApp, max_bytes: int = MAX_UPLOAD_BYTES) -> None:
        self.app = app
        self.max_bytes = max_bytes

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        received = 0

        async def limited_receive() -> Message:
            nonlocal received
            message = await receive()
            if message["type"] == "http.request":
                received += len(message.get("body", b""))
                if received > self.max_bytes:
                    raise PayloadTooLarge(f"request body exceeds {self.max_bytes} bytes")
            return message

        try:
            await self.app(scope, limited_receive, send)
        except PayloadTooLarge:
            await send({"type": "http.response.start", "status": 413, "headers": [(b"content-type", b"text/plain")]})
            await send({"type": "http.response.body", "body": b"payload too large"})
