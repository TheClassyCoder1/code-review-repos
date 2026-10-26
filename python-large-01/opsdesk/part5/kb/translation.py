"""Decides how stale or missing article translations get produced, and dispatches them."""

import enum
import logging
from dataclasses import dataclass, field
from typing import Any

from django.conf import settings

from .models import Document, Revision

log = logging.getLogger(__name__)


class TranslationTrigger(enum.Enum):
    MARK_READY_FOR_L10N = "mark_ready_for_l10n"
    TRANSLATION_STALE = "translation_stale"
    MANUAL = "manual"


class TranslationMethod(enum.Enum):
    AI = "ai"
    HUMAN = "human"


@dataclass
class TranslationRequest:
    revision: Revision
    trigger: TranslationTrigger
    target_locale: str
    method: TranslationMethod
    asynchronous: bool = True
    metadata: dict[str, Any] = field(default_factory=dict)


class TranslationStrategyFactory:
    """Maps a document/locale pair to a method and runs the matching strategy."""

    def __init__(self, ai_locales=None):
        self.ai_locales = set(ai_locales if ai_locales is not None else getattr(settings, "KB_AI_LOCALES", ()))

    def get_method_for_document(self, document: Document, locale: str) -> TranslationMethod:
        if locale in self.ai_locales:
            return TranslationMethod.AI
        return TranslationMethod.HUMAN

    def execute(self, request: TranslationRequest) -> None:
        if request.method is TranslationMethod.HUMAN:
            log.info(
                "Queued human translation of revision %s into %s", request.revision.pk, request.target_locale
            )
            return
        from .tasks import translate_revision

        payload = {
            "revision_id": request.revision.pk,
            "trigger": request.trigger.value,
            "target_locale": request.target_locale,
            "method": request.method.value,
            "metadata": request.metadata,
        }
        if request.asynchronous:
            translate_revision.delay(payload)
        else:
            translate_revision(payload)


class TranslationCandidateProcessor:
    def __init__(self, strategy_factory: TranslationStrategyFactory | None = None):
        self.strategy_factory = strategy_factory or TranslationStrategyFactory()

    def process(
        self,
        candidates: list[tuple[Document, Document | None, str]],
        trigger: TranslationTrigger,
    ) -> list[tuple[Document, Document | None, str]]:
        """Process translation candidates using appropriate strategies.

        Args:
            candidates: List of tuples (english_doc, translation_doc, locale)
            trigger: The trigger type for this translation
        Returns:
            The list of processed candidates
        """
        for english_doc, translation_doc, locale in candidates:
            translation_method = self.strategy_factory.get_method_for_document(english_doc, locale)

            metadata = {
                "english_revision_date": english_doc.latest_localizable_revision.created.isoformat()
            }

            if translation_doc:
                metadata.update(
                    {
                        "stale_translation_update": True,
                        "previous_translation_revision_id": translation_doc.current_revision.id
                        if translation_doc.current_revision
                        else None,
                        "translation_revision_date": translation_doc.current_revision.created.isoformat()
                        if translation_doc.current_revision
                        else None,
                    }
                )

            l10n_request = TranslationRequest(
                revision=english_doc.latest_localizable_revision,
                trigger=trigger,
                target_locale=locale,
                method=translation_method,
                asynchronous=True,
                metadata=metadata,
            )
            self.strategy_factory.execute(l10n_request)
        return candidates
