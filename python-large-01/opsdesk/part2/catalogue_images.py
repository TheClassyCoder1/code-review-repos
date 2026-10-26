"""Matches uploaded product images to catalogue products and attaches them."""

import hashlib
import logging
import os
import shutil

from django.core.exceptions import FieldError
from django.core.files import File
from django.db.transaction import atomic
from django.utils.translation import gettext_lazy as _
from PIL import Image

from opsdesk.backoffice.catalogue_import import ProductImageImporter
from opsdesk.part2.catalogue_models import Product, ProductImage

IMAGE_EXTENSIONS = (".jpg", ".jpeg", ".png", ".gif", ".webp")


class ImageImportError(Exception):
    """An image in the batch could not be imported; the batch is rolled back."""


class IdenticalImageError(Exception):
    """The product already has an image with the same content."""


class InvalidImageArchive(Exception):
    """The upload is neither a directory nor a readable tar/zip archive."""


class ProductImageImportHandler(ProductImageImporter):
    """Imports images named after a product's UPC, e.g. ``<upc>.jpg``."""

    _field = "upc"
    logger = logging.getLogger("opsdesk.part2.catalogue_images")

    def _get_image_files(self, dirname):
        image_dir = self._extract_images(dirname)
        names = sorted(os.listdir(image_dir)) if image_dir else []
        return image_dir, [n for n in names if n.lower().endswith(IMAGE_EXTENSIONS)]

    def _get_lookup_value_from_filename(self, filename):
        return os.path.splitext(filename)[0]

    def _process_image(self, dirname, filename, lookup_value):
        item = Product._default_manager.get(upc=lookup_value)
        with open(os.path.join(dirname, filename), "rb") as fh:
            Image.open(fh).verify()
            fh.seek(0)
            checksum = hashlib.sha256(fh.read()).hexdigest()
            if item.images.filter(checksum=checksum).exists():
                raise IdenticalImageError()
            fh.seek(0)
            ProductImage.objects.create(product=item, original=File(fh, name=filename), checksum=checksum)

    @atomic
    def handle(self, dirname):
        stats = {"num_processed": 0, "num_skipped": 0, "num_invalid": 0}
        image_dir, filenames = self._get_image_files(dirname)
        if image_dir:
            for filename in filenames:
                try:
                    lookup_value = self._get_lookup_value_from_filename(filename)
                    self._process_image(image_dir, filename, lookup_value)
                    stats["num_processed"] += 1
                except Product.MultipleObjectsReturned:
                    self.logger.warning(
                        "Multiple products matching %s='%s',"
                        " skipping" % (self._field, lookup_value)
                    )
                    stats["num_skipped"] += 1
                except Product.DoesNotExist:
                    self.logger.warning(
                        "No item matching %s='%s'" % (self._field, lookup_value)
                    )
                    stats["num_skipped"] += 1
                except IdenticalImageError:
                    self.logger.warning(
                        "Identical image already exists for"
                        " %s='%s', skipping" % (self._field, lookup_value)
                    )
                    stats["num_skipped"] += 1
                except IOError as e:
                    stats["num_invalid"] += 1
                    raise ImageImportError(
                        _("%(filename)s is not a valid image (%(error)s)")
                        % {"filename": filename, "error": e}
                    )
                except FieldError as e:
                    raise ImageImportError(e)
            if image_dir != dirname:
                shutil.rmtree(image_dir)
        else:
            raise InvalidImageArchive(_("%s is not a valid image archive") % dirname)
        self.logger.info(
            "Finished image import: %(num_processed)d imported,"
            " %(num_skipped)d skipped" % stats
        )
