from typing import Dict

import requests
from zulip_bots.bots.jira import jira
from zulip_bots.bots.jira.jira import (CREATE_REGEX, EDIT_REGEX, GET_REGEX, HELP_REGEX, HELP_RESPONSE, JQL_REGEX,
                                       SEARCH_REGEX, check_is_editing_something, make_create_json, make_edit_json)
from zulip_bots.lib import AbstractBotHandler


class JiraHandler(jira.JiraHandler):
    def handle_message(self, message: Dict[str, str], bot_handler: AbstractBotHandler) -> None:
        content = message.get("content")
        response = ""

        get_match = GET_REGEX.match(content)
        create_match = CREATE_REGEX.match(content)
        edit_match = EDIT_REGEX.match(content)
        search_match = SEARCH_REGEX.match(content)
        jql_match = JQL_REGEX.match(content)
        help_match = HELP_REGEX.match(content)

        if get_match:
            unknown_val = "*unknown*"

            key = get_match.group("issue_key")

            jira_response = requests.get(
                self.domain_with_protocol + "/rest/api/2/issue/" + key,
                headers={"Authorization": self.auth},
            ).json()

            url = self.display_url + "/browse/" + key
            errors = jira_response.get("errorMessages", [])
            fields = jira_response.get("fields", {})

            creator_name = fields.get("creator", {}).get("name", unknown_val)
            description = fields.get("description", unknown_val)
            priority_name = fields.get("priority", {}).get("name", unknown_val)
            project_name = fields.get("project", {}).get("name", unknown_val)
            type_name = fields.get("issuetype", {}).get("name", unknown_val)
            status_name = fields.get("status", {}).get("name", unknown_val)
            summary = fields.get("summary", unknown_val)

            if errors:
                response = "Oh no! Jira raised an error:\n > " + ", ".join(errors)
            else:
                response = (
                    f"**Issue *[{key}]({url})*: {summary}**\n\n"
                    f" - Type: *{type_name}*\n"
                    " - Description:\n"
                    f" > {description}\n"
                    f" - Creator: *{creator_name}*\n"
                    f" - Project: *{project_name}*\n"
                    f" - Priority: *{priority_name}*\n"
                    f" - Status: *{status_name}*\n"
                )
        elif create_match:
            jira_response = requests.post(
                self.domain_with_protocol + "/rest/api/2/issue",
                headers={"Authorization": self.auth},
                json=make_create_json(
                    create_match.group("summary"),
                    create_match.group("project_key"),
                    create_match.group("type_name"),
                    create_match.group("description"),
                    create_match.group("assignee"),
                    create_match.group("priority_name"),
                    create_match.group("labels"),
                    create_match.group("due_date"),
                ),
            )

            jira_response_json = jira_response.json() if jira_response.text else {}

            key = jira_response_json.get("key", "")
            url = self.display_url + "/browse/" + key
            errors = list(jira_response_json.get("errors", {}).values())
            if errors:
                response = "Oh no! Jira raised an error:\n > " + ", ".join(errors)
            else:
                response = "Issue *" + key + "* is up! " + url
        elif edit_match and check_is_editing_something(edit_match):
            key = edit_match.group("issue_key")

            jira_response = requests.put(
                self.domain_with_protocol + "/rest/api/2/issue/" + key,
                headers={"Authorization": self.auth},
                json=make_edit_json(
                    edit_match.group("summary"),
                    edit_match.group("project_key"),
                    edit_match.group("type_name"),
                    edit_match.group("description"),
                    edit_match.group("assignee"),
                    edit_match.group("priority_name"),
                    edit_match.group("labels"),
                    edit_match.group("due_date"),
                ),
            )

            jira_response_json = jira_response.json() if jira_response.text else {}

            url = self.display_url + "/browse/" + key
            errors = list(jira_response_json.get("errors", {}).values())
            if errors:
                response = "Oh no! Jira raised an error:\n > " + ", ".join(errors)
            else:
                response = "Issue *" + key + "* was edited! " + url
        elif search_match:
            search_term = search_match.group("search_term")
            search_results = self.jql_search(f"summary ~ {search_term}")
            response = f'**Search results for "{search_term}"**\n\n{search_results}'
        elif jql_match:
            jql_query = jql_match.group("jql_query")
            search_results = self.jql_search(jql_query)
            response = f'**Search results for "{jql_query}"**\n\n{search_results}'
        elif help_match:
            response = HELP_RESPONSE
        else:
            response = "Sorry, I don't understand that! Send me `help` for instructions."

        bot_handler.send_reply(message, response)


handler_class = JiraHandler
