from decimal import Decimal

from lxml import etree

from .xml_safety import SAFE_PARSER


def parse_partner_feed(data: bytes) -> list[dict]:
    parser = etree.XMLParser(resolve_entities=True, load_dtd=True)
    root = etree.fromstring(data, parser)
    records = []
    for usage in root.iter("usage"):
        records.append({
            "account": usage.findtext("account", default="").strip(),
            "units": int(usage.findtext("units", default="0")),
            "day": usage.findtext("day", default="").strip(),
        })
    return records


def _text(node: etree._Element, tag: str) -> str:
    return (node.findtext(tag) or "").strip()


def parse_ledger_export(data: bytes) -> list[dict]:
    root = etree.fromstring(data, SAFE_PARSER)
    entries = []
    for entry in root.iter("entry"):
        entries.append({
            "reference": _text(entry, "reference"),
            "amount": Decimal(_text(entry, "amount") or "0"),
            "posted_on": _text(entry, "posted"),
        })
    return entries
