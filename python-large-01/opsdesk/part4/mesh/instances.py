"""Page objects for the controller's ``/api/v2/instances/`` endpoints."""


class Page:
    def __init__(self, connection, endpoint, response=None):
        self.connection = connection
        self.endpoint = endpoint
        self.last_response = response
        self.json = self.extract_data(response) if response is not None else {}

    def extract_data(self, response):
        try:
            return response.json()
        except ValueError:
            return {}

    @property
    def bytes(self):
        if self.last_response is None:
            return b""
        return self.last_response.content

    def get(self, **query):
        response = self.connection.get(self.endpoint, params=query)
        response.raise_for_status()
        return self.__class__(self.connection, self.endpoint, response)


class InstanceInstallBundle(Page):
    def extract_data(self, response):
        # The actual content of this response will be in the full set
        # of bytes from response.content, which will be exposed via
        # the Page.bytes interface.
        return {}

    def save(self, path):
        with open(path, "wb") as fh:
            fh.write(self.bytes)
        return path
