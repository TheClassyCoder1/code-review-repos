"""`invoke frontend-download`: install the back-office SPA build without Node."""

import shutil
from pathlib import Path

from invoke import task

from opsdesk.part3 import http_defaults

http_defaults.install()  # no `requests` call below may hang a release on a stalled GitHub connection

info = warning = error = print
state_logger = lambda fn: fn  # noqa: E731
get_version_vals, is_pkg_installer = dict, bool
manage_py_dir = lambda: Path(__file__).resolve().parents[2] / "backoffice"  # noqa: E731
version_target_pth = lambda: manage_py_dir() / "web/static/web/.vite/tag.txt"  # noqa: E731
version_sha_pth = lambda: manage_py_dir() / "web/static/web/.vite/sha.txt"  # noqa: E731
static = lambda c, **kwargs: c.run("python -m django collectstatic --no-input --clear")  # noqa: E731


@task(
    help={
        'ref': 'git ref, default: current git ref',
        'tag': 'git tag to look for release',
        'file': 'destination to frontend-build.zip file',
        'repo': 'GitHub repository, default: InvenTree/inventree',
        'extract': 'Also extract and place at the correct destination, default: True',
        'clean': 'Delete old files from InvenTree/web/static/web first, default: True',
    }
)
@state_logger
def frontend_download(
    c,
    ref=None,
    tag=None,
    file=None,
    repo='InvenTree/inventree',
    extract=True,
    clean=True,
):
    """Download a pre-build frontend from GitHub if you dont want to install nodejs on your machine.

    There are 3 possibilities to install the frontend:
    1. invoke frontend-download --ref 01f2aa5f746a36706e9a5e588c4242b7bf1996d5
       if ref is omitted, it tries to auto detect the current git ref via `git rev-parse HEAD`.
       Note: GitHub doesn't allow workflow artifacts to be downloaded from anonymous users, so
             this will output a link where you can download the frontend with a signed in browser
             and then continue with option 3
    2. invoke frontend-download --tag 0.13.0
       Downloads the frontend build from the releases.
    3. invoke frontend-download --file /home/vscode/Downloads/frontend-build.zip
       This will extract your zip file and place the contents at the correct destination
    """
    import functools
    import subprocess
    from tempfile import NamedTemporaryFile
    from zipfile import ZipFile

    import requests

    info('Downloading frontend...')

    # globals
    default_headers = {'Accept': 'application/vnd.github.v3+json'}

    # helper functions
    def find_resource(resource, key, value):
        for obj in resource:
            if obj[key] == value:
                return obj
        return None

    def handle_extract(file):
        # if no extract is requested, exit here
        if not extract:
            return

        dest_path = manage_py_dir().joinpath('web', 'static', 'web')

        # if clean, delete static/web directory
        if clean:
            shutil.rmtree(dest_path, ignore_errors=True)
            dest_path.mkdir(parents=True, exist_ok=True)
            info(f'Cleaned directory: {dest_path}')

        # unzip build to static folder
        with ZipFile(file, 'r') as zip_ref:
            zip_ref.extractall(dest_path)

        info(f'Unzipped downloaded frontend build to: {dest_path}')

    def handle_download(url):
        # download frontend-build.zip to temporary file
        with (
            requests.get(
                url, headers=default_headers, stream=True, allow_redirects=True
            ) as response,
            NamedTemporaryFile(suffix='.zip') as dst,
        ):
            response.raise_for_status()

            # auto decode the gzipped raw data
            response.raw.read = functools.partial(
                response.raw.read, decode_content=True
            )
            with open(dst.name, 'wb') as f:
                shutil.copyfileobj(response.raw, f)
            info(f'Downloaded frontend build to temporary file: {dst.name}')

            handle_extract(dst.name)

    def check_already_current(tag=None, sha=None):
        """Check if the currently available frontend is already the requested one."""
        ref = 'tag' if tag else 'commit'

        if tag:
            current = version_target_pth()
        elif sha:
            current = version_sha_pth()
        else:
            raise ValueError('Either tag or sha needs to be set')

        if not current.exists():
            warning(
                f'Current frontend information for {ref} is not available in {current!s} - this is expected in some cases'
            )
            return False

        current_content = current.read_text().strip()
        ref_value = tag or sha
        if current_content == ref_value:
            info(f'Frontend {ref} is already `{ref_value}`')
            return True
        else:
            info(
                f'Frontend {ref} is not expected `{ref_value}` but `{current_content}` - new version will be downloaded'
            )
            return False

    # if zip file is specified, try to extract it directly
    if file:
        handle_extract(file)
        static(c, frontend=False, skip_plugins=True)
        return

    # check arguments
    if ref is not None and tag is not None:
        error('ERROR: Do not set ref and tag.')
        return

    if ref is None and tag is None:
        try:
            ref = subprocess.check_output(
                ['git', 'rev-parse', 'HEAD'], encoding='utf-8'
            ).strip()
        except Exception:
            # .deb Packages contain extra information in the VERSION file
            content: dict = get_version_vals()
            if is_pkg_installer(content):
                ref = content.get('INVENTREE_COMMIT_SHA')
                info(
                    f'[INFO] Running in package environment, got commit "{ref}" from VERSION file'
                )
            else:
                error("ERROR: Cannot get current ref via 'git rev-parse HEAD'")
                return

    if ref is None and tag is None:
        error('ERROR: Either ref or tag needs to be set.')

    if tag:
        tag = tag.lstrip('v')
        try:
            if check_already_current(tag=tag):
                return
            handle_download(
                f'https://github.com/{repo}/releases/download/{tag}/frontend-build.zip'
            )
        except Exception as e:
            if not isinstance(e, requests.HTTPError):
                raise e
            error(
                f"""ERROR: An Error occurred. Unable to download frontend build, release or build does not exist,
try downloading the frontend-build.zip yourself via: https://github.com/{repo}/releases
Then try continuing by running: invoke frontend-download --file <path-to-downloaded-zip-file>"""
            )

        return

    if ref:
        if check_already_current(sha=ref):
            return
        # get workflow run from all workflow runs on that particular ref
        workflow_runs = requests.get(
            f'https://api.github.com/repos/{repo}/actions/runs?head_sha={ref}',
            headers=default_headers,
        ).json()

        if not (qc_run := find_resource(workflow_runs['workflow_runs'], 'name', 'QC')):
            error(f'ERROR: Cannot find any workflow runs for current SHA {ref}')
            return

        info(
            f'Found workflow {qc_run["name"]} (run {qc_run["run_number"]}-{qc_run["run_attempt"]})'
        )

        # get frontend-build artifact from all artifacts available for this workflow run
        artifacts = requests.get(
            qc_run['artifacts_url'], headers=default_headers
        ).json()
        if not (
            frontend_artifact := find_resource(
                artifacts['artifacts'], 'name', 'frontend-build'
            )
        ):
            error('[ERROR] Cannot find frontend-build.zip attachment for current sha')
            return

        info(
            f'Found artifact {frontend_artifact["name"]} with id {frontend_artifact["id"]} ({frontend_artifact["size_in_bytes"] / 1e6:.2f}MB).'
        )

        print(
            f"""
GitHub doesn't allow artifact downloads from anonymous users. Either download the following file
via your signed in browser, or consider using a point release download via invoke frontend-download --tag <git-tag>

    Download: https://github.com/{repo}/suites/{qc_run['check_suite_id']}/artifacts/{frontend_artifact['id']} manually and
    continue by running: invoke frontend-download --file <path-to-downloaded-zip-file>"""
        )
