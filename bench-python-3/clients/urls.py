from django.urls import path
from rest_framework.routers import DefaultRouter

from . import views

router = DefaultRouter()
router.register("", views.ClientViewSet, basename="client")

urlpatterns = [
    path("import/", views.import_client_bundle, name="client-import"),
    path("intake/resume/", views.resume_intake, name="intake-resume"),
    path("docket/import/", views.import_docket, name="docket-import"),
] + router.urls
