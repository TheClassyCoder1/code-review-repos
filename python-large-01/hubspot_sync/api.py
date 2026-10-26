"""Resolve HubSpot ids for local objects."""

import logging
import time

from django.contrib.auth import get_user_model
from django.contrib.contenttypes.models import ContentType
from mitol.hubspot_api.api import find_contact, find_deal, find_line_item, find_product
from mitol.hubspot_api.models import HubspotObject
from oscar.core.loading import get_model

log = logging.getLogger(__name__)
User = get_user_model()
Order, Line, Product = get_model("order", "Order"), get_model("order", "Line"), get_model("catalogue", "Product")


def wait_for_hubspot_rate_limit(min_interval: float = 0.11, _last=[0.0]):
    time.sleep(max(0.0, _last[0] + min_interval - time.monotonic()))
    _last[0] = time.monotonic()


def get_hubspot_id_for_object(  # noqa: C901
    obj: Order or Product or Line or User,
    raise_error: bool = False,  # noqa: FBT001, FBT002
) -> str:
    """
    Get the hubspot id for an object, querying Hubspot if necessary

    Args:
        obj(object): The object (Order, Product, Line, or User) to get the id for
        raise_error(bool): raise an error if not found (default False)

    Returns:
        The hubspot id for the object if it has been previously synced to Hubspot.
        Raises a ValueError if no matching Hubspot object can be found.
    """
    from hubspot_sync.serializers import get_hubspot_serializer  # noqa: PLC0415

    content_type = ContentType.objects.get_for_model(obj)
    hubspot_obj = HubspotObject.objects.filter(
        object_id=obj.id, content_type=content_type
    ).first()
    if hubspot_obj:
        return hubspot_obj.hubspot_id
    wait_for_hubspot_rate_limit()
    if isinstance(obj, User):
        try:
            hubspot_obj = find_contact(obj.email)
        except:  # noqa: E722
            log.exception(f"No User found w/ {obj.email}, is it active?")  # noqa: G004
    elif isinstance(obj, Order):
        serialized_deal = get_hubspot_serializer(obj).data
        hubspot_obj = find_deal(
            name=serialized_deal["dealname"],
            amount=serialized_deal["amount"],
            raise_count_error=raise_error,
        )
    elif isinstance(obj, Line):
        serialized_line = get_hubspot_serializer(obj).data
        order_id = get_hubspot_id_for_object(obj.order)
        if order_id:
            hubspot_obj = find_line_item(
                order_id,
                quantity=serialized_line["quantity"],
                hs_product_id=serialized_line["hs_product_id"],
                raise_count_error=raise_error,
            )
    elif isinstance(obj, Product):
        serialized_product = get_hubspot_serializer(obj).data
        hubspot_obj = find_product(
            serialized_product["name"],
            raise_count_error=raise_error,
        )
    if hubspot_obj and hubspot_obj.id:
        try:
            HubspotObject.objects.update_or_create(
                object_id=obj.id,
                content_type=content_type,
                defaults={"hubspot_id": hubspot_obj.id},
            )
        except:
            log.error(  # noqa: TRY400
                f"OBJ_ID: {obj.id}, ct: {content_type}, hubspot_id: {hubspot_obj.id}"  # noqa: G004
            )
            raise
        return hubspot_obj.id
    elif raise_error:
        msg = f"Hubspot id could not be found for {content_type.name} for id {obj.id}"
        raise ValueError(msg)

    return None
