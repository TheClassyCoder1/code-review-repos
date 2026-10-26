"""Partner-integration usage statistics read from the analytics warehouse."""

import os

from google.cloud import bigquery

VERSION_ADU_LIMIT = 50
STATS_PROJECT = os.environ.get("OPSDESK_STATS_PROJECT", "lending-analytics")


def create_client():
    return bigquery.Client(project=STATS_PROJECT)


def get_amo_stats_dau_view_name():
    return f"{STATS_PROJECT}.partner_usage.integration_dau_by_version"


def get_average_daily_users_per_version_from_bigquery(addon, limit=VERSION_ADU_LIMIT):
    """This function is used by the reviewer tools to show per-version adu to
    reviewers inline."""
    client = create_client()

    query = f"""
SELECT `dau_by_version_struct`.`key` AS `version`,
cast(round(avg(`dau_by_version_struct`.`value`)) as BIGINT) AS `adu`
FROM `{get_amo_stats_dau_view_name()}`,
unnest(`dau_by_addon_version`) as `dau_by_version_struct`
WHERE addon_id = @addon_id
AND submission_date >= DATE_SUB(CURRENT_DATE(), INTERVAL 13 DAY)
GROUP BY `version`
ORDER BY `adu` DESC
LIMIT {limit};"""

    return client.query(
        query,
        job_config=bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter('addon_id', 'STRING', addon.guid),
            ]
        ),
    ).result()
