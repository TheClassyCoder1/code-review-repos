"""Editing form for knowledge-base article metadata."""

from django import forms
from django.conf import settings

from .models import Document
from .tasks import add_short_links


class DocumentForm(forms.ModelForm):
    class Meta:
        model = Document
        fields = ["title", "slug", "category", "needs_change", "needs_change_comment", "products", "topics"]

    def clean_slug(self):
        slug = self.cleaned_data["slug"]
        if "/" in slug:
            raise forms.ValidationError("Slugs cannot contain a slash.")
        return slug

    def save(self, parent_doc, **kwargs):
        """Persist the Document form, and return the saved Document."""
        doc = super().save(commit=False, **kwargs)
        doc.parent = parent_doc

        # If document doesn't need change, clear out the comment.
        if not doc.needs_change:
            doc.needs_change_comment = ""

        # Create the share link if it doesn't exist and is in
        # a category it should show for.
        doc.save()
        if doc.category in settings.IA_DEFAULT_CATEGORIES and not doc.share_link:
            # This operates under the constraints of passing in a list.
            add_short_links.delay([doc.pk])

        self.save_m2m()

        if parent_doc:
            # Products are not set on translations.
            doc.products.remove(*list(doc.products.all()))
            # A child always inherits parent topics.
            doc.topics.add(*list(parent_doc.topics.all()))

        return doc
