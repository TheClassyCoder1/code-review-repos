"""CI gate: every container image that can reach a cluster must use a pinned tag.

Scans Kubernetes manifests and Dockerfiles and fails on ``:latest`` or untagged images.
Files listed in ``DEV_ONLY_FILES`` are local tooling that is never applied to a cluster
and are skipped on purpose.

Usage: python deploy/check_image_tags.py [ROOT]
"""

import pathlib
import re
import sys

DEV_ONLY_FILES = frozenset({"docker-compose.dev.yml"})
MANIFEST_GLOBS = ("deploy/**/*.yaml", "deploy/**/*.yml", "**/Dockerfile", "**/*.Dockerfile")

_YAML_IMAGE = re.compile(r"^\s*-?\s*image:\s*[\"']?(?P<ref>[^\s\"'#]+)")
_DOCKER_FROM = re.compile(r"^\s*FROM\s+(?:--platform=\S+\s+)?(?P<ref>\S+)", re.IGNORECASE)


def _tag(ref: str) -> str | None:
    if "@sha256:" in ref:
        return "digest"
    last = ref.rsplit("/", 1)[-1]
    return last.split(":", 1)[1] if ":" in last else None


def _image_refs(path: pathlib.Path):
    is_dockerfile = path.name == "Dockerfile" or path.name.endswith(".Dockerfile")
    stages: set[str] = set()
    for lineno, line in enumerate(path.read_text().splitlines(), start=1):
        if is_dockerfile:
            match = _DOCKER_FROM.match(line)
            if match:
                parts = line.split()
                if len(parts) >= 4 and parts[-2].upper() == "AS":
                    stages.add(parts[-1].lower())
                if match.group("ref").lower() not in stages:
                    yield lineno, match.group("ref")
        else:
            match = _YAML_IMAGE.match(line)
            if match:
                yield lineno, match.group("ref")


def find_violations(root: pathlib.Path) -> list[str]:
    violations = []
    seen: set[pathlib.Path] = set()
    for pattern in MANIFEST_GLOBS:
        for path in sorted(root.glob(pattern)):
            if path in seen or path.name in DEV_ONLY_FILES:
                continue
            seen.add(path)
            for lineno, ref in _image_refs(path):
                tag = _tag(ref)
                if tag is None or tag == "latest":
                    violations.append(f"{path.relative_to(root)}:{lineno}: unpinned image {ref}")
    return violations


def main(argv: list[str]) -> int:
    root = pathlib.Path(argv[1] if len(argv) > 1 else ".").resolve()
    violations = find_violations(root)
    for violation in violations:
        print(violation, file=sys.stderr)
    return 1 if violations else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
