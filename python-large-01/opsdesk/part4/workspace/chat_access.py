"""Access checks for the ops chat export (incident channels)."""

from django.db.models import Exists, OuterRef, QuerySet
from zerver.lib.stream_subscription import has_channel_content_access_helper
from zerver.lib.user_groups import UserGroupMembershipDetails
from zerver.models import Message, Stream, UserMessage, UserProfile


def bulk_access_stream_messages_query(
    user_profile: UserProfile, messages: QuerySet[Message], stream: Stream
) -> QuerySet[Message]:
    """This function mirrors bulk_access_messages, above, but applies the
    limits to a QuerySet and returns a new QuerySet which only
    contains messages in the given stream which the user can access.
    Note that this only works with streams.  It may return an empty
    QuerySet if the user has access to no messages (for instance, for
    a private stream which the user is not subscribed to).

    This logic is mirrored in zerver.lib.narrow.get_base_query_for_search.
    """

    assert stream.recipient_id is not None
    messages = messages.filter(realm_id=user_profile.realm_id, recipient_id=stream.recipient_id)

    if stream.is_public() and user_profile.can_access_public_streams():
        return messages

    user_group_membership_details = UserGroupMembershipDetails(user_recursive_group_ids=None)
    has_content_access = has_channel_content_access_helper(
        stream, user_profile, user_group_membership_details, is_subscribed=None
    )

    if not has_content_access:
        return Message.objects.none()
    if not stream.is_history_public_to_subscribers():
        messages = messages.alias(
            has_usermessage=Exists(
                UserMessage.objects.filter(
                    user_profile_id=user_profile.id, message_id=OuterRef("id")
                )
            )
        ).filter(has_usermessage=True)
    return messages
