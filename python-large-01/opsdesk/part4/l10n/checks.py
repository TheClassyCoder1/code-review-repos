"""Runs compare-locales checks against a proposed translation."""

from compare_locales.parser.base import Comment as CommentEntity
from compare_locales.parser.dtd import DTDEntity as CompareDTDEntity
from compare_locales.parser.properties import PropertiesEntity as ComparePropertiesEntity
from fluent.syntax import FluentParser as _FluentSyntaxParser
from fluent.syntax.ast import Junk

from opsdesk.part4.l10n.models import Entity


class UnsupportedStringError(Exception):
    """The translation could not be parsed into an entity of its resource's format."""


class UnsupportedResourceTypeError(Exception):
    """compare-locales has no checker for this resource format."""


class FluentParser:
    """Stream-style adapter over ``fluent.syntax`` matching the other parsers."""

    def __init__(self):
        self._entries = []

    def readUnicode(self, source: str):
        self._entries = [e for e in _FluentSyntaxParser(with_spans=False).parse(source).body]

    def __iter__(self):
        return iter(self._entries)


def cast_to_compare_locales(format: str, entity: Entity, string: str):
    """
    Cast a Pontoon's translation object into Entities supported by `compare-locales`.

    :arg basestring resource_ext: extension of a resource.
    :arg pontoon.base.models.Entity entity: Source entity
    :arg basestring string: a translation
    :return: source entity and translation entity that will be compatible with
        a compare-locales checker. Type of those entities depends on the resource_ext.
    """
    cl_key = entity.key[0] if entity and entity.key else ""
    if format == "properties":
        return (
            ComparePropertiesEntity(
                cl_key, entity.string, CommentEntity(entity.comment)
            ),
            ComparePropertiesEntity(cl_key, string, CommentEntity(entity.comment)),
        )

    elif format == "dtd":
        return (
            CompareDTDEntity(cl_key, entity.string, CommentEntity(entity.comment)),
            CompareDTDEntity(cl_key, string, CommentEntity(entity.comment)),
        )

    elif format == "fluent":
        parser = FluentParser()

        parser.readUnicode(entity.string)
        (refEntity,) = list(parser)

        parser.readUnicode(string)
        trEntity = list(parser)[0] if list(parser) else None

        if not trEntity or isinstance(trEntity, Junk):
            raise UnsupportedStringError(format)

        return (
            refEntity,
            trEntity,
        )

    raise UnsupportedResourceTypeError(format)
