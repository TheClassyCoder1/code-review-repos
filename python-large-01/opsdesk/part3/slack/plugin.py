import io
import json
import logging

from slack_sdk.errors import SlackApiError
from sqlalchemy.orm import Session

from opsdesk.part3.slack.messaging import SlackAPIErrorCode, create_slack_client, send_message

logger = logging.getLogger(__name__)
Case = object  # ORM model lives in the case service


def _blocks(**kwargs):
    case_id = kwargs.get("case_id") or kwargs["case"].id
    return [{"type": "section", "text": {"type": "mrkdwn", "text": f"Case `{case_id}`"}}]


create_case_message = create_signal_message = create_action_buttons_message = _blocks
create_genai_signal_analysis_message = lambda **kwargs: ("No analysis available.", [])  # noqa: E731


class SlackConversationPlugin:
    def __init__(self, configuration):
        self.configuration = configuration

    def create_threaded(self, case: Case, conversation_id: str, db_session: Session):
        """Creates a new threaded conversation."""
        client = create_slack_client(self.configuration)
        blocks = create_case_message(case=case, channel_id=conversation_id)
        response = send_message(client=client, conversation_id=conversation_id, blocks=blocks)
        response_timestamp = response["timestamp"]

        if case.signal_instances:
            signal_response = None

            # we try to generate a GenAI signal analysis message
            try:
                message, message_blocks = create_genai_signal_analysis_message(
                    case=case,
                    db_session=db_session,
                )
                if message and isinstance(message, dict):
                    # we update the genai_analysis field in the case model with the message if it's a dict
                    # if the message is a string, it means there was an error generating the analysis
                    case.genai_analysis = message

                if message_blocks:
                    signal_response = send_message(
                        client=client,
                        conversation_id=conversation_id,
                        ts=response_timestamp,
                        blocks=message_blocks,
                    )
            except Exception as e:
                logger.exception(f"Error generating GenAI signal analysis message: {e}")

            case.signal_thread_ts = (
                signal_response.get("timestamp") if signal_response else response_timestamp
            )

            # we try to generate a signal message
            try:
                message = create_signal_message(
                    case_id=case.id, channel_id=conversation_id, db_session=db_session
                )
                signal_response = send_message(
                    client=client,
                    conversation_id=conversation_id,
                    ts=case.signal_thread_ts,
                    blocks=message,
                )
                if signal_response:
                    case.signal_thread_ts = signal_response.get("timestamp")
            except Exception as e:
                logger.exception(f"Error generating signal message: {e}")

            # we try to upload the alert JSON to the case thread
            try:
                client.files_upload_v2(
                    channel=signal_response.get(
                        "id"
                    ),  # we need the conversation ID not the name here
                    thread_ts=case.signal_thread_ts,
                    file=io.BytesIO(json.dumps(case.signal_instances[0].raw, indent=4).encode()),
                )
            except SlackApiError as e:
                if e.response["error"] == SlackAPIErrorCode.MISSING_SCOPE:
                    exception_message = (
                        "Error uploading alert JSON to the case thread due to a missing scope"
                    )
                else:
                    exception_message = "Error uploading alert JSON to the case thread"
                logger.exception(f"{exception_message}: {e}")

            except Exception as e:
                logger.exception(f"Error uploading alert JSON to the case thread: {e}")

            # we try to generate action buttons
            try:
                message = create_action_buttons_message(
                    case=case, channel_id=conversation_id, db_session=db_session
                )
                send_message(
                    client=client,
                    conversation_id=conversation_id,
                    ts=case.signal_thread_ts,
                    blocks=message,
                )
            except Exception as e:
                logger.exception(f"Error generating action buttons message: {e}")

            db_session.commit()
        return response
