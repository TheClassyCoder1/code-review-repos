"""Loan product catalogue: product classes with typed, per-class attributes."""

from django.db import models

ATTRIBUTE_TYPES = [
    ("text", "Text"),
    ("integer", "Integer"),
    ("float", "Float"),
    ("boolean", "True / False"),
    ("date", "Date"),
]


class ProductClass(models.Model):
    name = models.CharField(max_length=128)
    slug = models.SlugField(unique=True)

    class Meta:
        app_label = "backoffice"

    def __str__(self):
        return self.name


class ProductAttribute(models.Model):
    product_class = models.ForeignKey(ProductClass, related_name="attributes", on_delete=models.CASCADE)
    name = models.CharField(max_length=128)
    code = models.SlugField(max_length=128)
    type = models.CharField(max_length=20, choices=ATTRIBUTE_TYPES, default="text")
    required = models.BooleanField(default=False)

    class Meta:
        app_label = "backoffice"
        unique_together = [("product_class", "code")]


class Product(models.Model):
    product_class = models.ForeignKey(ProductClass, related_name="products", on_delete=models.PROTECT)
    title = models.CharField(max_length=255)
    slug = models.SlugField(max_length=255)
    description = models.TextField(blank=True)
    is_public = models.BooleanField(default=True)

    class Meta:
        app_label = "backoffice"


class ProductAttributeValue(models.Model):
    product = models.ForeignKey(Product, related_name="attribute_values", on_delete=models.CASCADE)
    attribute = models.ForeignKey(ProductAttribute, on_delete=models.CASCADE)
    value_text = models.TextField(blank=True, null=True)
    value_integer = models.IntegerField(blank=True, null=True)
    value_float = models.FloatField(blank=True, null=True)
    value_boolean = models.BooleanField(blank=True, null=True)
    value_date = models.DateField(blank=True, null=True)

    class Meta:
        app_label = "backoffice"
        unique_together = [("product", "attribute")]

    def _get_value(self):
        return getattr(self, "value_%s" % self.attribute.type)

    def _set_value(self, new_value):
        setattr(self, "value_%s" % self.attribute.type, new_value)

    value = property(_get_value, _set_value)
