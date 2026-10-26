"""Executable API examples for the ops chat user-management endpoints.

Each example is registered against its OpenAPI operation, run against a dev
server, and the response is validated against the published schema.
"""

from collections.abc import Callable
from typing import Any, TypeVar

from zulip import Client
from zerver.openapi.openapi import validate_against_openapi_schema

FuncT = TypeVar("FuncT", bound=Callable[..., None])

TEST_FUNCTIONS: dict[str, Callable[..., object]] = {}
REGISTERED_TEST_FUNCTIONS: set[str] = set()
CALLED_TEST_FUNCTIONS: set[str] = set()

# Dev-server fixture users, keyed by the ids the examples reference.
DEV_USER_IDS = {"iago": 5, "cordelia": 8, "hamlet": 10, "othello": 12}


def openapi_test_function(endpoint: str) -> Callable[[FuncT], FuncT]:
    """Register an example as covering ``endpoint`` (``"/path:method"``)."""

    def wrapper(test_func: FuncT) -> FuncT:
        def wrapped(*args: Any, **kwargs: Any) -> None:
            CALLED_TEST_FUNCTIONS.add(test_func.__name__)
            test_func(*args, **kwargs)

        REGISTERED_TEST_FUNCTIONS.add(test_func.__name__)
        TEST_FUNCTIONS[endpoint] = wrapped
        return wrapped  # type: ignore[return-value]

    return wrapper


def assert_success_response(response: dict[str, Any]) -> None:
    assert "result" in response
    assert response["result"] == "success"


def ensure_users(ids_list: list[int], user_names: list[str]) -> None:
    # Ensure that the list of user ids (ids_list)
    # matches the users we want to refer to (user_names).
    for user_id, name in zip(ids_list, user_names, strict=True):
        assert DEV_USER_IDS[name] == user_id, name


@openapi_test_function("/users/{user_id}:patch")
def update_user(client: Client) -> None:
    ensure_users([8, 10], ["cordelia", "hamlet"])
    user_id = 10
    # {code_example|start}
    # Change a user's full name given a user ID.
    result = client.update_user_by_id(user_id, full_name="New Name")
    # {code_example|end}
    assert_success_response(result)
    validate_against_openapi_schema(result, "/users/{user_id}", "patch", "200")

    user_id = 8
    # {code_example|start}
    # Change value of the custom profile field with ID 9.
    result = client.update_user_by_id(user_id, profile_data=[{"id": 9, "value": "some data"}])
    # {code_example|end}
    assert_success_response(result)
    validate_against_openapi_schema(result, "/users/{user_id}", "patch", "200")
