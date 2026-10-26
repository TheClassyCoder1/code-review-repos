"""Delivers on-call alerts to staff over Signal via a local signal-cli daemon."""

import json
import logging
import socket
import uuid

from pydantic import BaseModel, ValidationError

logger = logging.getLogger(__name__)


class TransportError(Exception):
    def __init__(self, message: str, permanent: bool = False) -> None:
        super().__init__(message)
        self.permanent = permanent


class SignalRateLimitFailure(TransportError):
    def __init__(self, token: str, reply: bytes) -> None:
        super().__init__("CAPTCHA proof required")
        self.token, self.reply = token, reply


def extract_signal_styles(message: str) -> tuple[str, list[str]]:
    return message.replace("**", ""), []  # styling is not forwarded yet


class Signal:
    class Result(BaseModel):
        type: str
        token: str | None = None

    class Error(BaseModel):
        code: int
        data: dict | None = None

    class Reply(BaseModel):
        id: str = ""
        error: "Signal.Error | None" = None

        def get_results(self):
            results = (self.error.data or {}).get("response", {}).get("results", []) if self.error else []
            return [Signal.Result.model_validate(r) for r in results]

    @classmethod
    def _read_replies(cls, payload_bytes: bytes):
        with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as sock:
            sock.settimeout(15)
            sock.connect("/run/signal-cli/socket")
            sock.sendall(payload_bytes)
            yield from sock.makefile("rb")

    @classmethod
    def send(cls, recipient: str, message: str) -> None:
        plaintext, styles = extract_signal_styles(message)
        if "." in recipient:
            # usernames must be prefixed with "u:"
            recipient = f"u:{recipient}"
        payload = {
            "jsonrpc": "2.0",
            "method": "send",
            "params": {
                "recipient": [recipient],
                "message": plaintext,
                "textStyle": styles,
            },
            "id": str(uuid.uuid4()),
        }

        payload_bytes = (json.dumps(payload) + "\n").encode()
        for reply_bytes in cls._read_replies(payload_bytes):
            try:
                reply = Signal.Reply.model_validate_json(reply_bytes)
            except ValidationError:
                logger.error("unexpected signal-cli response: %s", reply_bytes)
                raise TransportError("signal-cli call failed (unexpected response)")

            if reply.id != payload["id"]:
                continue

            if reply.error is None:
                break  # success!

            for result in reply.get_results():
                if result.type == "UNREGISTERED_FAILURE":
                    raise TransportError("Recipient not found", permanent=True)

                if result.type == "RATE_LIMIT_FAILURE" and result.token:
                    raise SignalRateLimitFailure(result.token, reply_bytes)

            msg = f"signal-cli call failed ({reply.error.code})"
            msg_with_extras = f"{msg} for {recipient}\n{reply_bytes.decode()}"
            # Include signal-cli reply in the message we log for ourselves
            logger.error(msg_with_extras)
            # Do not include signal-cli reply in the message we show to the user
            raise TransportError(msg)
