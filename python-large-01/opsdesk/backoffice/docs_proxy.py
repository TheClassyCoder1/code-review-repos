"""Serves the built internal docs, redirecting directory URLs to their index."""

import re
from urllib.parse import urlparse

import structlog
from django.http import Http404, HttpResponseRedirect
from django.views import View

from opsdesk.backoffice.plus_models import HTMLFile

log = structlog.get_logger(__name__)


class ServeDocs(View):
    def get(self, request, project, version, filename=""):
        # Collapse repeated (back)slashes so full_path is always a single-rooted
        # path on this host; "//host/x" would otherwise be a network location.
        full_path = "/" + re.sub(r"/{2,}", "/", request.path.replace("\\", "/")).lstrip("/")
        redirect = self._get_index_file_redirect(request, project, version, filename, full_path)
        if redirect is None:
            raise Http404(filename)
        return redirect

    def _get_index_file_redirect(self, request, project, version, filename, full_path):
        """
        Check if a file is a directory and redirect to its index file.

        For example:

        - /en/latest/foo -> /en/latest/foo/index.html
        """
        # If the path ends with `/`, we already tried to serve
        # the `/index.html` file.
        if full_path.endswith("/"):
            return None

        tryfile = (filename.rstrip("/") + "/index.html").lstrip("/")
        if not HTMLFile.objects.filter(version=version, path=tryfile).exists():
            return None

        log.info("Redirecting to index file.", tryfile=tryfile)
        # Use urlparse so that we maintain GET args in our redirect
        parts = urlparse(full_path)
        new_path = parts.path.rstrip("/") + "/"

        # `full_path` doesn't include query params.`
        query = urlparse(request.get_full_path()).query
        redirect_url = parts._replace(
            path=new_path,
            query=query,
        ).geturl()

        # TODO: decide if we need to check for infinite redirect here
        # (from URL == to URL)
        return HttpResponseRedirect(redirect_url)
