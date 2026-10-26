"""Product images for the loan-product catalogue shown in the portal."""

from django.conf import settings
from django.db import models


class MissingProductImage:
    """Stands in for a product image when a product has none uploaded."""

    def __init__(self, name=None):
        self.name = name or getattr(settings, "OPSDESK_MISSING_IMAGE", "image_not_found.jpg")
        self.caption = ""
        self.is_missing = True


class LoanProduct(models.Model):
    code = models.SlugField(unique=True)
    title = models.CharField(max_length=255)

    class Meta:
        app_label = "backoffice"

    def get_missing_image(self):
        """
        Returns a missing image object.
        """
        # This class should have a 'name' property so it mimics the Django file
        # field.
        return MissingProductImage()

    def primary_image(self):
        image = self.images.order_by("display_order").first()
        return image if image is not None else self.get_missing_image()


class LoanProductImage(models.Model):
    product = models.ForeignKey(LoanProduct, on_delete=models.CASCADE, related_name="images")
    original = models.ImageField(upload_to="products/%Y/%m/")
    display_order = models.PositiveIntegerField(default=0)

    class Meta:
        app_label = "backoffice"
