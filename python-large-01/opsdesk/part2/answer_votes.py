"""Knowledge-base answer voting."""

from django_ratelimit.core import is_ratelimited as _is_ratelimited
from rest_framework import permissions, status, viewsets
from rest_framework.decorators import action
from rest_framework.exceptions import APIException
from rest_framework.response import Response

from opsdesk.part2.kb_models import Answer, AnswerVote


class GenericAPIException(APIException):
    def __init__(self, status_code, detail):
        self.status_code = status_code
        super().__init__(detail)


def is_ratelimited(request, name, rate):
    return _is_ratelimited(request, group=name, key="user_or_ip", rate=rate, method=["POST"], increment=True)


class AnswerViewSet(viewsets.GenericViewSet):
    queryset = Answer.objects.select_related("question")

    @action(detail=True, methods=["post"], permission_classes=[permissions.IsAuthenticated])
    def helpful(self, request, pk=None):
        if is_ratelimited(request, "answer-vote", "10/d"):
            raise GenericAPIException(
                status.HTTP_429_TOO_MANY_REQUESTS,
                "You've exceeded the number of votes for answers allowed in a day.",
            )

        answer = self.get_object()

        if not answer.question.editable:
            raise GenericAPIException(status.HTTP_403_FORBIDDEN, "Answer not editable")
        if answer.has_voted(request):
            raise GenericAPIException(status.HTTP_409_CONFLICT, "Cannot vote twice")

        AnswerVote(answer=answer, creator=request.user, helpful=True).save()
        num_helpful_votes = AnswerVote.objects.filter(answer=answer, helpful=True).count()
        num_unhelpful_votes = AnswerVote.objects.filter(answer=answer, helpful=False).count()
        return Response(
            {
                "num_helpful_votes": num_helpful_votes,
                "num_unhelpful_votes": num_unhelpful_votes,
            }
        )
