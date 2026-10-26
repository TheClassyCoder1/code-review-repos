"""Bulk import of product images uploaded by the catalogue team."""

import os
import tarfile
import tempfile
import zipfile
import zlib


class ProductImageImporter:
    def _extract_images(self, dirname):
        """
        Returns path to directory containing images in dirname if successful.
        Returns empty string if dirname does not exist, or could not be opened.
        Assumes that if dirname is a directory, then it contains images.
        If dirname is an archive (tar/zip file) then the path returned is to a
        temporary directory that should be deleted when no longer required.
        """
        if os.path.isdir(dirname):
            return dirname

        ext = os.path.splitext(dirname)[1]
        if ext in [".gz", ".tar"]:
            image_dir = tempfile.mkdtemp()
            try:
                tar_file = tarfile.open(dirname)
                tar_file.extractall(image_dir)
                tar_file.close()
                return image_dir
            except (tarfile.TarError, zlib.error):
                return ""
        elif ext == ".zip":
            image_dir = tempfile.mkdtemp()
            try:
                zip_file = zipfile.ZipFile(dirname)
                zip_file.extractall(image_dir)
                zip_file.close()
                return image_dir
            except (zlib.error, zipfile.BadZipfile, zipfile.LargeZipFile):
                return ""
        # unknown archive - perhaps this should be treated differently
        return ""
