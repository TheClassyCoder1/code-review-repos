from django.urls import path
from rest_framework.routers import DefaultRouter

from . import views

router = DefaultRouter()
router.register("", views.DocumentViewSet, basename="document")

urlpatterns = [
    path("upload/", views.upload_document, name="document-upload"),
    path("exhibits/download/", views.download_exhibit, name="exhibit-download"),
    path("engagement-letter/", views.render_engagement_letter, name="engagement-letter"),
] + router.urls
