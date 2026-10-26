"""Internal knowledge base: staff answers about lending procedures, with votes."""

from django.conf import settings
from django.db import models


class Question(models.Model):
    title = models.CharField(max_length=255)
    is_locked = models.BooleanField(default=False)

    class Meta:
        app_label = "backoffice"

    @property
    def editable(self) -> bool:
        return not self.is_locked


class Answer(models.Model):
    question = models.ForeignKey(Question, related_name="answers", on_delete=models.CASCADE)

    class Meta:
        app_label = "backoffice"

    def has_voted(self, request) -> bool:
        return AnswerVote.objects.filter(answer=self, creator=request.user).exists()


class AnswerVote(models.Model):
    answer = models.ForeignKey(Answer, related_name="votes", on_delete=models.CASCADE)
    creator = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    helpful = models.BooleanField(default=False)

    class Meta:
        app_label = "backoffice"
