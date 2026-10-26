"""RFC 7807 error responses for the back-office v3 API."""

from django.core.exceptions import PermissionDenied
from django.core.exceptions import ValidationError as DjangoValidationError
from django.http import Http404, HttpRequest, JsonResponse
from ninja import NinjaAPI
from ninja.errors import HttpError
from ninja.errors import ValidationError as NinjaValidationError
from pydantic import ValidationError as PydanticValidationError


class FormValidationError(DjangoValidationError):
    """Validation failure carrying per-field (path, [(message, code)]) pairs."""

    def __init__(self, loc_errors):
        self.loc_errors = loc_errors
        super().__init__([message for _path, coded in loc_errors for message, _code in coded])


class RichTextFormatError(ValueError):
    """A note body that is not valid for the declared rich-text format."""


def problem_response(status: int, detail: str, **extra) -> JsonResponse:
    body = {"type": "about:blank", "status": status, "detail": detail, **extra}
    return JsonResponse(body, status=status, content_type="application/problem+json")


def register_exception_handlers(api: NinjaAPI):
    """Map API exceptions to RFC 7807 ``application/problem+json`` responses.

    Validation errors use status 422 (Unprocessable Entity), which is the usual
    RFC 7807 choice for request validation failures.
    """

    @api.exception_handler(PydanticValidationError)
    @api.exception_handler(NinjaValidationError)
    @api.exception_handler(DjangoValidationError)
    def validation_error_handler(
        request: HttpRequest,
        exc: FormValidationError
        | DjangoValidationError
        | NinjaValidationError
        | PydanticValidationError,
    ):
        if isinstance(exc, FormValidationError):
            errors = [
                {"type": code, "loc": list(path), "msg": message}
                for path, coded_messages in exc.loc_errors
                for message, code in coded_messages
            ]
        elif isinstance(exc, DjangoValidationError):
            errors = [{"msg": msg} for msg in exc.messages]
        elif isinstance(exc, NinjaValidationError):
            errors = exc.errors
        else:  # PydanticValidationError
            # Ninja's validation error replaces Pydantic's input hints with a
            # more user-friendly version, and is already handled above. We use
            # Pydantic directly for internal validation logic, and its "input"
            # hints may be irrelevant to API consumers, so remove them.
            errors = [
                {key: value for key, value in error.items() if key != "input"}
                for error in exc.errors()
            ]
        return problem_response(
            status=422,
            detail="Validation failed",
            errors=errors,
        )

    @api.exception_handler(PermissionDenied)
    def permission_denied_handler(request: HttpRequest, exc: PermissionDenied):
        # v3 never trusts session auth: 401 unless a bearer token resolved.
        if not request.user.is_authenticated:
            return problem_response(status=401, detail="Authentication required")
        return problem_response(status=403, detail=str(exc) or "Permission denied")

    @api.exception_handler(Http404)
    def not_found_handler(request: HttpRequest, exc: Http404):
        return problem_response(status=404, detail=str(exc) or "Not found")

    @api.exception_handler(HttpError)
    def http_error_handler(request: HttpRequest, exc: HttpError):
        return problem_response(status=exc.status_code, detail=str(exc))

    @api.exception_handler(RichTextFormatError)
    def rich_text_format_error_handler(request: HttpRequest, exc: RichTextFormatError):
        return problem_response(status=400, detail=str(exc))

    @api.exception_handler(Exception)
    def unhandled_exception_handler(request: HttpRequest, exc: Exception):
        from django.conf import settings

        if not settings.DEBUG:
            raise exc
        return problem_response(status=500, detail=str(exc))
