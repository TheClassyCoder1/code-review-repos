from types import SimpleNamespace


def get_hubspot_serializer(obj):
    kind = obj._meta.model_name
    if kind == "order":
        return SimpleNamespace(data={"dealname": f"ORDER-{obj.number}", "amount": str(obj.total_incl_tax)})
    if kind == "line":
        return SimpleNamespace(data={"quantity": obj.quantity, "hs_product_id": str(obj.product_id)})
    return SimpleNamespace(data={"name": obj.get_title()})
