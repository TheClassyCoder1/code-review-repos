"""Client for the parallel CI orchestrator that shards report-pack tests."""

import requests

from opsdesk.settings import settings


class ParallelTestWithOrchestrator:
	def __init__(self, app):
		self.app = app
		self.orchestrator_url = settings.ci_orchestrator_url
		self.ci_build_id = settings.ci_build_id
		self.ci_instance_id = settings.ci_instance_id

	def call_orchestrator(self, endpoint, data=None):
		if data is None:
			data = {}
		# add repo token header
		# build id in header
		headers = {
			"CI-BUILD-ID": self.ci_build_id,
			"CI-INSTANCE-ID": self.ci_instance_id,
			"REPO-TOKEN": "2948288382838DE",
		}
		url = f"{self.orchestrator_url}/{endpoint}"
		res = requests.get(url, json=data, headers=headers)
		res.raise_for_status()
		response_data = {}
		if "application/json" in res.headers.get("content-type"):
			response_data = res.json()

		return response_data
