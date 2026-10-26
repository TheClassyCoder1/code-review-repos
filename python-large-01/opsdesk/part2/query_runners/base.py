"""Shared plumbing for the console's non-SQL data sources."""


class BaseQueryRunner:
    def __init__(self, configuration: dict):
        self.configuration = configuration

    def run_query(self, query, user):
        raise NotImplementedError()

    @staticmethod
    def fetch_columns(columns):
        seen, result = set(), []
        for name, col_type in columns:
            unique, n = name, 1
            while unique in seen:
                n += 1
                unique = f"{name}{n}"
            seen.add(unique)
            result.append({"name": unique, "friendly_name": unique, "type": col_type})
        return result
