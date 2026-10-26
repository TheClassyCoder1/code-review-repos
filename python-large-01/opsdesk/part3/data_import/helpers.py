import requests
from django.db import models

from opsdesk.part3.labeling.models import ReviewProject, Task, flag_set  # noqa: F401

FF_BROS_1092_IMPORT_UNKNOWN_COMPLETED_BY = "fflag_fix_bros_1092_import_unknown_completed_by"
WebhookAction = type("WebhookAction", (), {"TASKS_CREATED": "TASKS_CREATED"})


class ProjectImport(models.Model):
    Status = models.TextChoices("Status", "CREATED IN_PROGRESS FAILED COMPLETED")

    project = models.ForeignKey(ReviewProject, on_delete=models.CASCADE)
    status = models.CharField(max_length=32, default="CREATED")
    url = models.URLField(null=True)
    preannotated_from_fields = models.JSONField(null=True)
    commit_to_project = models.BooleanField(default=True)
    return_task_ids = models.BooleanField(default=False)
    error = models.TextField(null=True)
    task_count = models.IntegerField(default=0)
    annotation_count = models.IntegerField(default=0)
    prediction_count = models.IntegerField(default=0)
    duration = models.FloatField(default=0)
    file_upload_ids = models.JSONField(default=list)
    found_formats = models.JSONField(default=dict)
    data_columns = models.JSONField(default=list)
    task_ids = models.JSONField(default=list)


class ProjectSummary(models.Model):
    project = models.OneToOneField(ReviewProject, on_delete=models.CASCADE)
    update_data_columns = lambda self, tasks: None  # noqa: E731


def load_tasks_for_async_import(project_import, user):
    tasks = requests.get(project_import.url, timeout=(5, 120)).json()
    return tasks, [], {".json": len(tasks)}, sorted({k for t in tasks for k in t})


class ImportApiSerializer:
    def __init__(self, data, many=True, context=None):
        self.initial_data, self.db_annotations, self.db_predictions = data, [], []
        self.is_valid = lambda raise_exception=False: True

    def save(self, project_id):
        return [Task.objects.create(project_id=project_id) for _ in self.initial_data]


def reformat_predictions(tasks, fields, project, raise_errors=False):
    return [{"data": t, "predictions": [{"result": t.pop(f)} for f in fields if f in t]} for t in tasks]


class LabelInterface:
    def __init__(self, label_config):
        self.label_config = label_config

    def validate_prediction(self, prediction, return_errors=False):
        return [] if isinstance(prediction.get("result"), (list, dict)) else ["result must be a list or object"]


sanitize_prediction_import_payload = dict


def emit_webhooks_for_instance(organization, project, action, instances):
    """Partner webhooks are not configured for review projects yet."""
