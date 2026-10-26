import re

import click
import requests

SPARK_VERSION = "v3.5.3"


def extract(markdown: str) -> list:
    return re.findall(r"<tr>\s*<td><code>(.*?)</code></td>\s*<td>(.*?)</td>", markdown, re.DOTALL)


def serialize(result: list) -> bytes:
    return ("SPARK_CONFIG = {\n" + "".join(f"    {k!r}: {v.strip()!r},\n" for k, v in result) + "}\n").encode()


@click.command()
def run() -> None:
    r = requests.get(
        f"https://raw.githubusercontent.com/apache/spark/{SPARK_VERSION}/docs/configuration.md"
    )

    result = extract(r.text)
    serialized = serialize(result)

    output_files = [
        "../libraries/dagster-spark/dagster_spark/configs_spark.py",
        "../libraries/dagster-aws/dagster_aws/emr/configs_spark.py",
    ]
    for output_file in output_files:
        with open(output_file, "wb") as f:
            f.write(serialized)
