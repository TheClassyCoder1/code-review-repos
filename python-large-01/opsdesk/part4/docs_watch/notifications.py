"""Turns docs-pipeline change events into watcher notifications.

``process_changes`` expects a change list that has already been through
``opsdesk.part4.docs_watch.webhook.validate_changes``.
"""

import re
from collections import defaultdict

from django.db import models, transaction

from opsdesk.backoffice.plus_models import DocumentURL

COPY = {
    "added_stable": "Supported in ",
    "removed_stable": "Removed from ",
    "added_preview": "In development in ",
}

BROWSER_NAMES = {
    "chrome": "Chrome",
    "chrome_android": "Chrome Android",
    "firefox": "Firefox",
    "firefox_android": "Firefox for Android",
    "safari": "Safari",
    "safari_ios": "Safari on iOS",
    "webview_android": "WebView Android",
}

# Browsers that share an engine and release train are announced together.
BROWSER_GROUP = {
    "firefox": "firefox",
    "firefox_android": "firefox",
    "chrome": "chrome",
    "chrome_android": "chrome",
    "webview_android": "chrome",
    "safari": "safari",
    "safari_ios": "safari",
}


class WatchedPage(models.Model):
    url = models.CharField(max_length=250, unique=True)
    path = models.CharField(max_length=500, blank=True)

    class Meta:
        app_label = "backoffice"


class DocsNotification(models.Model):
    page = models.ForeignKey(WatchedPage, on_delete=models.CASCADE)
    text = models.CharField(max_length=500)
    data = models.JSONField(null=True)
    created = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "backoffice"


def get_browser_info(browser, preview=False):
    name = BROWSER_NAMES.get(browser, browser)
    return name + " (preview)" if preview else name


def pluralize(browser_list):
    browser_list = list(browser_list)
    if len(browser_list) <= 1:
        return "".join(browser_list)
    return ", ".join(browser_list[:-1]) + " and " + browser_list[-1]


@transaction.atomic
def publish_bcd_notification(path, text, data=None):
    for page in WatchedPage.objects.filter(path=path):
        DocsNotification.objects.create(page=page, text=text, data=data)


@transaction.atomic
def publish_content_notification(url, text, data=None):
    page, _ = WatchedPage.objects.get_or_create(url=url)
    DocsNotification.objects.create(page=page, text=text, data=data)


def process_changes(changes):
    bcd_notifications = []
    content_notifications = []

    for change in changes:
        if change["event"] in ["added_stable", "removed_stable", "added_preview"]:
            groups = defaultdict(list)
            for browser_data in change["browsers"]:
                browser = get_browser_info(
                    browser_data["browser"],
                    change["event"] == "added_preview",
                )
                groups[BROWSER_GROUP.get(browser_data["browser"], browser)].append(
                    {
                        "browser": f"{browser} {browser_data['version']}",
                        "data": change,
                    }
                )
            for group in groups.values():
                browser_list = pluralize([i["browser"] for i in group])
                bcd_notifications.append(
                    {
                        "path": change["path"],
                        "text": COPY[change["event"]] + browser_list,
                        "data": [i["data"] for i in group],
                    }
                )

        elif change["event"] == "added_subfeatures":
            n = len(change["subfeatures"])
            bcd_notifications.append(
                {
                    "path": change["path"],
                    "text": f"{n} compatibility subfeature{'s'[:n ^ 1]} added",
                    "data": change,
                }
            )
        elif change["event"] == "added_nonnull":
            browser_list = [
                get_browser_info(i["browser"]) for i in change["support_changes"]
            ]
            text = pluralize(browser_list)
            bcd_notifications.append(
                {
                    "path": change["path"],
                    "text": f"More complete compatibility data added for {text}",
                    "data": change,
                }
            )

        elif change["event"] == "content_updated":
            url = DocumentURL.normalize_uri(change["page_url"])
            m = re.match(r"^https://github.com/(.+)/pull/(\d+)$", change["pr_url"])
            content_notifications.append(
                {
                    "url": url,
                    "text": f"Page updated (see PR!{m.group(1)}!{m.group(2)}!!)",
                }
            )

    for notification in bcd_notifications:
        publish_bcd_notification(**notification)

    for notification in content_notifications:
        publish_content_notification(**notification)
