"""Sensor that launches downstream jobs when a loan-book asset materialises."""

import inspect
from collections.abc import Callable, Mapping, Sequence
from typing import Any

import dagster._check as check
from dagster._core.definitions.asset_key import AssetKey
from dagster._core.definitions.asset_sensor_definition import get_asset_sensor_param_names
from dagster._core.definitions.metadata import RawMetadataMapping
from dagster._core.definitions.resource_annotation import get_resource_args
from dagster._core.definitions.run_request import RunRequest, SkipReason
from dagster._core.definitions.sensor_definition import (
    DefaultSensorStatus,
    SensorDefinition,
    SensorReturnTypesUnion,
    validate_and_get_resource_dict,
)
from dagster._core.definitions.target import ExecutableDefinition
from dagster._core.definitions.utils import check_valid_name


class LoanBookAssetSensorDefinition(SensorDefinition):
    def __init__(
        self,
        name: str,
        asset_key: AssetKey,
        job_name: str | None,
        asset_materialization_fn: Callable[
            ...,
            SensorReturnTypesUnion,
        ],
        minimum_interval_seconds: int | None = None,
        description: str | None = None,
        job: ExecutableDefinition | None = None,
        jobs: Sequence[ExecutableDefinition] | None = None,
        default_status: DefaultSensorStatus = DefaultSensorStatus.STOPPED,
        required_resource_keys: set[str] | None = None,
        tags: Mapping[str, str] | None = None,
        metadata: RawMetadataMapping | None = None,
    ):
        self._asset_key = check.inst_param(asset_key, "asset_key", AssetKey)
        self._asset_materialization_fn = asset_materialization_fn
        self._job_name = job_name

        from dagster._core.event_api import AssetRecordsFilter

        resource_arg_names: set[str] = {
            arg.name for arg in get_resource_args(asset_materialization_fn)
        }

        combined_required_resource_keys = (
            check.opt_set_param(required_resource_keys, "required_resource_keys", of_type=str)
            | resource_arg_names
        )
        self._raw_required_resource_keys = combined_required_resource_keys

        def _wrap_asset_fn(materialization_fn) -> Any:
            def _fn(context) -> Any:
                after_cursor = None
                if context.cursor:
                    try:
                        after_cursor = int(context.cursor)
                    except ValueError:
                        after_cursor = None

                event_records = context.instance.fetch_materializations(
                    AssetRecordsFilter(
                        asset_key=self._asset_key,
                        after_storage_id=after_cursor,
                    ),
                    ascending=False,
                    limit=1,
                ).records

                if not event_records:
                    yield SkipReason(
                        f"No new materialization events found for asset key {self._asset_key}"
                    )
                    return

                event_record = event_records[0]

                (
                    context_param_name,
                    event_log_entry_param_name,
                ) = get_asset_sensor_param_names(materialization_fn)

                resource_args_populated = validate_and_get_resource_dict(
                    context.resources, name, resource_arg_names
                )

                # Build asset sensor function args, which can include any subset of
                # context arg, event log entry arg, and any resource args
                args = resource_args_populated
                if context_param_name:
                    args[context_param_name] = context
                if event_log_entry_param_name:
                    args[event_log_entry_param_name] = event_record.event_log_entry

                result = materialization_fn(**args)
                if inspect.isgenerator(result) or isinstance(result, list):
                    yield from result
                elif isinstance(result, (SkipReason, RunRequest)):
                    yield result
                context.update_cursor(str(event_record.storage_id))

            return _fn

        super().__init__(
            name=check_valid_name(name),
            job_name=job_name,
            evaluation_fn=_wrap_asset_fn(
                check.callable_param(asset_materialization_fn, "asset_materialization_fn"),
            ),
            minimum_interval_seconds=minimum_interval_seconds,
            description=description,
            job=job,
            jobs=jobs,
            default_status=default_status,
            required_resource_keys=combined_required_resource_keys,
            tags=tags,
            metadata=metadata,
        )
