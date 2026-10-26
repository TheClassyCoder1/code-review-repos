"""Back-office views for documentation builds and the docs proxy error handler."""

from django.contrib.auth.decorators import login_required
from django.http import HttpResponse, HttpResponseForbidden, HttpResponseRedirect
from django.shortcuts import get_object_or_404
from django.urls import reverse
from django.utils.decorators import method_decorator
from django.views.generic import View

from .models import AdminPermission, Project, cancel_build


class BuildCancelView(View):
    http_method_names = ["post"]

    @method_decorator(login_required)
    def post(self, request, project_slug, build_pk):
        project = get_object_or_404(Project, slug=project_slug)
        build = get_object_or_404(project.builds, pk=build_pk)

        if not AdminPermission.is_admin(request.user, project):
            return HttpResponseForbidden()

        cancel_build(build)

        return HttpResponseRedirect(
            reverse("builds_detail", args=[project.slug, build.pk]),
        )


def fast_404(request, *args, **kwargs):
    """
    A fast error page handler.

    This stops us from running RTD logic in our error handling. We already do
    this in RTD prod when we fallback to it.
    """
    return HttpResponse("Not Found.", status=404)
