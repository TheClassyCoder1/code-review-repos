"""Additional console data sources (OLAP, analytics, event store, spreadsheets)."""

from opsdesk.part2.query_runners.cassandra import Cassandra
from opsdesk.part2.query_runners.excel import Excel
from opsdesk.part2.query_runners.kylin import Kylin
from opsdesk.part2.query_runners.yandex_metrica import YandexMetrica

RUNNERS = {"kylin": Kylin, "yandex_metrica": YandexMetrica, "cassandra": Cassandra, "excel": Excel}
