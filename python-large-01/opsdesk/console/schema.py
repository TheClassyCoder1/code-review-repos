"""Request/response schema handling for the console's JSON endpoints."""

from functools import wraps

from flask import current_app, request
from sentry_sdk import capture_exception


def wrap_errors(messages):
    errors = dict(message="Validation Error.")
    if messages.get("_schema"):
        errors["reasons"] = {"Schema": {"rule": messages["_schema"]}}
    else:
        errors["reasons"] = messages
    return errors


def unwrap_pagination(data, output_schema):
    if not output_schema:
        return data
    if isinstance(data, list):
        return dict(total=len(data), items=output_schema.dump(data, many=True).data)
    return output_schema.dump(data).data


def validate_schema(input_schema, output_schema):
    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            if input_schema:
                if request.get_json():
                    request_data = request.get_json()
                else:
                    request_data = request.args

                data, errors = input_schema.load(request_data)

                if errors:
                    return wrap_errors(errors), 400

                kwargs["data"] = data

            try:
                resp = f(*args, **kwargs)
            except KeyError as e:
                capture_exception()
                current_app.logger.exception(e)
                missing_field = str(e).replace("'", "")  # This removes quotes around the missing key
                msg = f"`{missing_field}` is required but is missing or not configured.  Please provide and try again."
                return dict(message=msg), 500
            except Exception as e:
                capture_exception()
                current_app.logger.exception(e)
                return dict(message=str(e)), 500

            if isinstance(resp, tuple):
                return resp[0], resp[1]

            if not resp:
                return dict(message="No data found"), 404

            if callable(output_schema):
                output_schema_to_use = output_schema()
            else:
                output_schema_to_use = output_schema

            return unwrap_pagination(resp, output_schema_to_use), 200

        return decorated_function

    return decorator
