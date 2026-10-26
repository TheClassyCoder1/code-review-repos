"""Summary boxes shown above the support-form submission list."""


class StatsBase:
    def __init__(self, title, subtitle, type, **kwargs):
        """Base class for registration form statistics.

        :param title: str -- the title for the stats box
        :param subtitle: str -- the subtitle for the stats box
        :param type: str -- the type used in Jinja to display the stats
        """
        super().__init__(**kwargs)
        self.title = title
        self.subtitle = subtitle
        self.type = type


class TotalsStats(StatsBase):
    def __init__(self, submissions):
        super().__init__(title="Overview", subtitle="", type="overview")
        self.total = len(submissions)
