from pathlib import Path

import yaml

from .spec_loader import SpecLoader

BUILTIN_DIR = Path(__file__).resolve().parent / "builtin_templates"

REQUIRED_KEYS = ("name", "columns")


def load_uploaded_spec(raw: bytes) -> dict:
    spec = yaml.load(raw.decode("utf-8"), Loader=yaml.Loader)
    if not isinstance(spec, dict):
        raise ValueError("template spec must be a mapping")
    missing = [key for key in REQUIRED_KEYS if key not in spec]
    if missing:
        raise ValueError(f"template spec missing keys: {missing}")
    return spec


def _builtin_path(name: str) -> Path:
    return BUILTIN_DIR / f"{name}.yaml"


def list_builtin_templates() -> list[str]:
    return sorted(p.stem for p in BUILTIN_DIR.glob("*.yaml"))


def load_builtin_spec(name: str) -> dict:
    path = _builtin_path(name)
    text = path.read_text(encoding="utf-8")
    spec = yaml.load(text, Loader=SpecLoader)
    if not isinstance(spec, dict) or "columns" not in spec:
        raise ValueError(f"builtin template {name} is malformed")
    return spec
