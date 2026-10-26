"""Partner product catalogue (financed goods shown in dealer checkouts)."""

from django.db import models


class Product(models.Model):
    upc = models.CharField(max_length=64, unique=True, null=True, blank=True)
    title = models.CharField(max_length=255)

    class Meta:
        app_label = "backoffice"


class ProductImage(models.Model):
    product = models.ForeignKey(Product, related_name="images", on_delete=models.CASCADE)
    original = models.ImageField(upload_to="catalogue/images", max_length=255)
    checksum = models.CharField(max_length=64, db_index=True)

    class Meta:
        app_label = "backoffice"
