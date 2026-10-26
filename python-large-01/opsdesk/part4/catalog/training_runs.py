"""Seeds the staff-training catalogue with sample course runs for local stacks."""

from datetime import datetime, timedelta, timezone

FAKE_TAG_PREFIX = "fake-"


def now_in_utc():
    return datetime.now(tz=timezone.utc)


def generate_run_defaults(run):
    """Generates default values for a CourseRun based on the provided run data"""
    now = now_in_utc()
    run_defaults = {
        "run_tag": f"{FAKE_TAG_PREFIX}{run['run_tag']}{now.year}",
        "live": True,
        "title": run["title"],
        "upgrade_deadline": f"{now.year}{run['upgrade_deadline']}",
        "is_self_paced": run["is_self_paced"],
    }

    if run["is_enrollable"]:
        run_defaults["enrollment_start"] = f"{now.year}{run['enrollment_start']}"
        run_defaults["enrollment_end"] = f"{now.year}{run['enrollment_end']}"
        run_defaults["start_date"] = f"{now.year}{run['start_date']}"
        run_defaults["end_date"] = f"{now.year}{run['end_date']}"
    if run["is_archived"]:
        # If the run is archived, set the start and end dates to be in the past
        previous_year = (now - timedelta(days=365)).year
        run_defaults["start_date"] = f"{previous_year}{run['start_date']}"
        run_defaults["end_date"] = f"{previous_year}{run['end_date']}"
        run_defaults["enrollment_start"] = f"{previous_year}{run['enrollment_start']}"
        run_defaults["enrollment_end"] = f"{now.year}{run['enrollment_end']}"
        run_defaults["run_tag"] = f"{FAKE_TAG_PREFIX}{run['run_tag']}{previous_year}"

    return run_defaults


def build_seed(runs: list[dict]) -> list[dict]:
    """``runs`` come from the fixture JSON; dates there are ``-MM-DDTHH:MM:SSZ`` suffixes."""
    return [{"course": run["course"], **generate_run_defaults(run)} for run in runs]
