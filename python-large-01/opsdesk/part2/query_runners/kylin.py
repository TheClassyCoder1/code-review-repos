"""Apache Kylin OLAP cubes (portfolio vintage and roll-rate cubes)."""

import os

import requests
from requests.auth import HTTPBasicAuth

from opsdesk.part2.query_runners.base import BaseQueryRunner


class Kylin(BaseQueryRunner):
    def get_table_schema(self, table):
        name = table["table_NAME"]
        if table.get("table_SCHEM"):
            name = "{}.{}".format(table["table_SCHEM"], name)
        return {"name": name, "columns": [col["column_NAME"] for col in table["columns"]]}

    def get_schema(self, get_stats=False):
        url = self.configuration["url"]
        kylinuser = self.configuration["user"]
        kylinpass = self.configuration["password"]
        kylinproject = self.configuration["project"]

        resp = requests.get(
            os.path.join(url, "api/tables_and_columns"),
            params={"project": kylinproject},
            auth=HTTPBasicAuth(kylinuser, kylinpass),
        )

        resp.raise_for_status()

        data = resp.json()
        return [self.get_table_schema(table) for table in data]
