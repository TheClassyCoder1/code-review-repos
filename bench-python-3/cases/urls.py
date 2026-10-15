from django.urls import path
from rest_framework.routers import DefaultRouter

from . import views

router = DefaultRouter()
router.register("", views.CaseViewSet, basename="case")

urlpatterns = [
    path("<int:pk>/cover/", views.print_case_cover, name="case-cover"),
] + router.urls
