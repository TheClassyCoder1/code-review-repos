import json
import os
import subprocess


def write_lines(path: str, rows: list[dict]) -> int:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "a", encoding="utf-8") as fh:
        for row in rows:
            fh.write(json.dumps(row, default=str) + "\n")
    return len(rows)


def compress(path: str) -> str:
    subprocess.run(f"gzip -9 -f {path}", shell=True, check=True)
    return path + ".gz"
