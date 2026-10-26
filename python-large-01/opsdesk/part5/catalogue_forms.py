"""Dashboard form for editing loan products and their class-specific attributes."""

from django import forms
from django.core import exceptions

from .catalogue_models import Product, ProductAttributeValue

ATTRIBUTE_FIELD_FACTORIES = {
    "text": lambda attribute: forms.CharField(label=attribute.name, required=attribute.required),
    "integer": lambda attribute: forms.IntegerField(label=attribute.name, required=attribute.required),
    "float": lambda attribute: forms.FloatField(label=attribute.name, required=attribute.required),
    "boolean": lambda attribute: forms.BooleanField(label=attribute.name, required=False),
    "date": lambda attribute: forms.DateField(label=attribute.name, required=attribute.required),
}


class ProductForm(forms.ModelForm):
    class Meta:
        model = Product
        fields = ["title", "slug", "description", "is_public"]

    def __init__(self, product_class, *args, **kwargs):
        self.set_initial(product_class, kwargs)
        super().__init__(*args, **kwargs)
        if "instance" not in kwargs or kwargs["instance"] is None:
            self.instance.product_class = product_class
        self.product_class = product_class
        self.add_attribute_fields(product_class)

    def set_initial(self, product_class, kwargs):
        """
        Set initial data for the form. Sets the correct product structure
        and fetches initial values for the dynamically constructed attribute
        fields.
        """
        if "initial" not in kwargs:
            kwargs["initial"] = {}
        self.set_initial_attribute_values(product_class, kwargs)

    def set_initial_attribute_values(self, product_class, kwargs):
        """
        Update the kwargs['initial'] value to have the initial values based on
        the product instance's attributes
        """
        instance = kwargs.get("instance")
        if instance is None:
            return
        for attribute in product_class.attributes.all():
            try:
                value = instance.attribute_values.get(attribute=attribute).value
            except exceptions.ObjectDoesNotExist:
                pass
            else:
                kwargs["initial"]["attr_%s" % attribute.code] = value

    def add_attribute_fields(self, product_class):
        for attribute in product_class.attributes.all():
            factory = ATTRIBUTE_FIELD_FACTORIES.get(attribute.type)
            if factory is None:
                continue
            self.fields["attr_%s" % attribute.code] = factory(attribute)
