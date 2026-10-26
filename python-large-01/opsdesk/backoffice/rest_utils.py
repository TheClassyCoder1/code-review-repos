"""JSON view helpers for the back-office single-page screens."""

import functools
import json
import json as jsonutils
import logging

from django import http
from requests import HTTPError

LOG = logging.getLogger(__name__)

http_errors = (HTTPError, http.Http404)


class http_utils:
    @staticmethod
    def is_ajax(request):
        return request.headers.get("x-requested-with") == "XMLHttpRequest"


class JSONResponse(http.HttpResponse):
    def __init__(self, data, status=200, json_encoder=json.JSONEncoder):
        content = jsonutils.dumps(data, cls=json_encoder) if status != 204 else b""
        super().__init__(status=status, content=content, content_type="application/json")


def ajax(authenticated=True, data_required=False,
         json_encoder=json.JSONEncoder):
    """Decorator to allow the wrappered view to exist in an AJAX environment.

    Provide a decorator to wrap a view method so that it may exist in an
    entirely AJAX environment:

    - data decoded from JSON as input and data coded as JSON as output
    - result status is coded in the HTTP status code; any non-2xx response
      data will be coded as a JSON string, otherwise the response type (always
      JSON) is specific to the method called.

    if authenticated is true then we'll make sure the current user is
    authenticated.

    If data_required is true then we'll assert that there is a JSON body
    present.

    The wrapped view method should return either:

    - JSON serialisable data
    - an object of the django http.HttpResponse subclass (one of JSONResponse
      or CreatedResponse is suggested)
    - nothing

    Methods returning nothing (or None explicitly) will result in a 204 "NO
    CONTENT" being returned to the caller.
    """
    def decorator(function, authenticated=authenticated,
                  data_required=data_required):
        @functools.wraps(function,
                         assigned=functools.WRAPPER_ASSIGNMENTS)
        def _wrapped(self, request, *args, **kw):
            if authenticated and not request.user.is_authenticated:
                return JSONResponse('not logged in', 401)
            if not http_utils.is_ajax(request):
                return JSONResponse('request must be AJAX', 400)

            # decode the JSON body if present
            request.DATA = None
            if request.body:
                try:
                    request.DATA = jsonutils.loads(request.body)
                except (TypeError, ValueError) as e:
                    return JSONResponse('malformed JSON request: %s' % e, 400)

            if data_required:
                if not request.DATA:
                    return JSONResponse('request requires JSON body', 400)

            # invoke the wrapped function, handling exceptions sanely
            try:
                data = function(self, request, *args, **kw)
                if isinstance(data, http.HttpResponse):
                    return data
                if data is None:
                    return JSONResponse('', status=204)
                return JSONResponse(data, json_encoder=json_encoder)
            except http_errors as e:
                # exception was raised with a specific HTTP status
                for attr in ['http_status', 'code', 'status_code']:
                    if hasattr(e, attr):
                        http_status = getattr(e, attr)
                        break
                else:
                    LOG.exception('HTTP exception with no status/code')
                    return JSONResponse(str(e), 500)
                return JSONResponse(str(e), http_status)
            except Exception as e:
                LOG.exception('error invoking apiclient')
                return JSONResponse(str(e), 500)

        return _wrapped
    return decorator
