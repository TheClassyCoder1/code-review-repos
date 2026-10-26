"""Translation catalogues served to the back-office front end."""

import os

import orjson
from django.conf import settings
from django.http import JsonResponse
from django.utils import translation


def get_language_translation_data(language: str) -> dict[str, str]:
    if language == "en":
        return {}
    locale = translation.to_locale(language)
    path = os.path.join(settings.DEPLOY_ROOT, "locale", locale, "translations.json")
    try:
        with open(path, "rb") as reader:
            return orjson.loads(reader.read())
    except FileNotFoundError:
        print(f"Translation for {language} not found at {path}")
        return {}


def translations_view(request):
    language = request.GET.get("lang") or translation.get_language()
    return JsonResponse(get_language_translation_data(language))
