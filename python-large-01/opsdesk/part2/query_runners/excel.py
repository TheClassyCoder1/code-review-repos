"""Excel workbooks fetched by URL (partner remittance files, broker rate sheets)."""

import numpy as np
import pandas as pd
import yaml

from opsdesk.part2.query_runners.base import BaseQueryRunner
from opsdesk.part2.query_runners.net import UnacceptableAddressException, requests_or_advocate


class Excel(BaseQueryRunner):
    def run_query(self, query, user):
        path = ""
        ua = ""
        args = {}
        try:
            args = yaml.safe_load(query)
            path = args["url"]
            args.pop("url", None)
            ua = args["user-agent"]
            args.pop("user-agent", None)

        except Exception:
            pass

        try:
            response = requests_or_advocate.get(url=path, headers={"User-agent": ua})
            workbook = pd.read_excel(response.content, **args)

            df = workbook.copy()
            data = {"columns": [], "rows": []}
            conversions = [
                {
                    "pandas_type": np.integer,
                    "redash_type": "integer",
                },
                {
                    "pandas_type": np.inexact,
                    "redash_type": "float",
                },
                {
                    "pandas_type": np.datetime64,
                    "redash_type": "datetime",
                    "to_redash": lambda x: x.strftime("%Y-%m-%d %H:%M:%S"),
                },
                {"pandas_type": np.bool_, "redash_type": "boolean"},
                {"pandas_type": np.object_, "redash_type": "string"},
            ]
            labels = []
            for dtype, label in zip(df.dtypes, df.columns):
                for conversion in conversions:
                    if issubclass(dtype.type, conversion["pandas_type"]):
                        data["columns"].append(
                            {"name": label, "friendly_name": label, "type": conversion["redash_type"]}
                        )
                        labels.append(label)
                        func = conversion.get("to_redash")
                        if func:
                            df[label] = df[label].apply(func)
                        break
            data["rows"] = df[labels].replace({np.nan: None}).to_dict(orient="records")

            error = None
        except KeyboardInterrupt:
            error = "Query cancelled by user."
            data = None
        except UnacceptableAddressException:
            error = "Can't query private addresses."
            data = None
        except Exception as e:
            error = "Error reading {0}. {1}".format(path, str(e))
            data = None

        return data, error
