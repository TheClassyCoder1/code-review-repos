import logging
import time
from typing import Callable, Optional

from django.contrib.auth import get_user_model
from django.db import transaction

from opsdesk.part3.data_import.helpers import (FF_BROS_1092_IMPORT_UNKNOWN_COMPLETED_BY, ImportApiSerializer,
                                               LabelInterface, ProjectImport, ProjectSummary, WebhookAction,
                                               emit_webhooks_for_instance, flag_set, load_tasks_for_async_import,
                                               reformat_predictions, sanitize_prediction_import_payload)

logger = logging.getLogger(__name__)
User = get_user_model()


def _async_import_background_streaming(project_import, user):
    tasks = load_tasks_for_async_import(project_import, user)[0]
    for offset in range(0, len(tasks), 500):
        ImportApiSerializer(data=tasks[offset:offset + 500]).save(project_id=project_import.project_id)
    project_import.status = ProjectImport.Status.COMPLETED
    project_import.save()


def async_import_background(
    import_id, user_id, recalculate_stats_func: Optional[Callable[..., None]] = None, **kwargs
):
    with transaction.atomic():
        try:
            project_import = ProjectImport.objects.get(id=import_id)
        except ProjectImport.DoesNotExist:
            logger.error(f'ProjectImport with id {import_id} not found, import processing failed')
            return
        if project_import.status != ProjectImport.Status.CREATED:
            logger.error(f'Processing import with id {import_id} already started')
            return
        project_import.status = ProjectImport.Status.IN_PROGRESS
        project_import.save(update_fields=['status'])

    user = User.objects.get(id=user_id)

    if flag_set('fflag_fix_back_plt_902_async_import_background_oom_fix_22092025_short', user='auto'):
        logger.info(f'Using streaming import for project {project_import.project.id}')
        _async_import_background_streaming(project_import, user)
        return

    start = time.time()
    project = project_import.project
    tasks = None
    # upload files from request, and parse all tasks
    # TODO: Stop passing request to load_tasks function, make all validation before
    tasks, file_upload_ids, found_formats, data_columns = load_tasks_for_async_import(project_import, user)

    if project_import.preannotated_from_fields:
        # turn flat task JSONs {"column1": value, "column2": value} into {"data": {"column1"..}, "predictions": [{..."column2"}]
        raise_errors = flag_set(
            'fflag_feat_utc_210_prediction_validation_15082025', user=project.organization.created_by
        )
        logger.info(f'Reformatting predictions with raise_errors: {raise_errors}')
        tasks = reformat_predictions(tasks, project_import.preannotated_from_fields, project, raise_errors)

    # Always validate predictions regardless of commit_to_project setting
    if project.label_config_is_not_default and flag_set(
        'fflag_feat_utc_210_prediction_validation_15082025', user=project.organization.created_by
    ):
        validation_errors = []
        li = LabelInterface(project.label_config)

        for i, task in enumerate(tasks):
            if 'predictions' in task:
                for j, prediction in enumerate(task['predictions']):
                    try:
                        prediction = sanitize_prediction_import_payload(prediction)
                        validation_errors_list = li.validate_prediction(prediction, return_errors=True)
                        if validation_errors_list:
                            for error in validation_errors_list:
                                validation_errors.append(f'Task {i}, prediction {j}: {error}')
                    except Exception as e:
                        error_msg = f'Task {i}, prediction {j}: Error validating prediction - {str(e)}'
                        validation_errors.append(error_msg)
                        logger.error(f'Exception during validation: {error_msg}')

        if validation_errors:
            error_message = f'Prediction validation failed ({len(validation_errors)} errors):\n'
            for error in validation_errors:
                error_message += f'- {error}\n'

            if flag_set('fflag_feat_utc_210_prediction_validation_15082025', user=project.organization.created_by):
                project_import.error = error_message
                project_import.status = ProjectImport.Status.FAILED
                project_import.save(update_fields=['error', 'status'])
                return
            else:
                logger.error(
                    f'Prediction validation failed, not raising error - ({len(validation_errors)} errors):\n{error_message}'
                )

    if project_import.commit_to_project:
        with transaction.atomic():
            # Lock summary for update to avoid race conditions
            summary = ProjectSummary.objects.select_for_update().get(project=project)

            # Immediately create project tasks and update project states and counters
            # BROS-1092: pass user so BaseTaskSerializerBulk can attribute unknown
            # completed_by entries back to the importer instead of project.created_by.
            ctx = {'project': project}
            if flag_set(FF_BROS_1092_IMPORT_UNKNOWN_COMPLETED_BY, user=user):
                ctx['user'] = user
            serializer = ImportApiSerializer(data=tasks, many=True, context=ctx)
            serializer.is_valid(raise_exception=True)

            try:
                tasks = serializer.save(project_id=project.id)
                emit_webhooks_for_instance(user.active_organization, project, WebhookAction.TASKS_CREATED, tasks)

                task_count = len(tasks)
                annotation_count = len(serializer.db_annotations)
                prediction_count = len(serializer.db_predictions)
                # Update counters (like total_annotations) for new tasks and after bulk update tasks stats. It should be a
                # single operation as counters affect bulk is_labeled update

                recalculate_stats_counts = {
                    'task_count': task_count,
                    'annotation_count': annotation_count,
                    'prediction_count': prediction_count,
                }

                project.update_tasks_counters_and_task_states(
                    tasks_queryset=tasks,
                    maximum_annotations_changed=False,
                    overlap_cohort_percentage_changed=False,
                    tasks_number_changed=True,
                    recalculate_stats_counts=recalculate_stats_counts,
                )
                logger.info('Tasks bulk_update finished (async import)')

                summary.update_data_columns(tasks)
                # TODO: summary.update_created_annotations_and_labels
            except Exception as e:
                # Handle any other unexpected errors during task creation
                error_message = f'Error creating tasks: {str(e)}'
                project_import.error = error_message
                project_import.status = ProjectImport.Status.FAILED
                project_import.save(update_fields=['error', 'status'])
                return
    else:
        # Do nothing - just output file upload ids for further use
        task_count = len(tasks)
        annotation_count = None
        prediction_count = None

    duration = time.time() - start

    project_import.task_count = task_count or 0
    project_import.annotation_count = annotation_count or 0
    project_import.prediction_count = prediction_count or 0
    project_import.duration = duration
    project_import.file_upload_ids = file_upload_ids
    project_import.found_formats = found_formats
    project_import.data_columns = data_columns
    if project_import.return_task_ids:
        project_import.task_ids = [task.id for task in tasks]

    project_import.status = ProjectImport.Status.COMPLETED
    project_import.save()
