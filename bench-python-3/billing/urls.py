from django.urls import path
from rest_framework.routers import DefaultRouter

from . import views

router = DefaultRouter()
router.register("time-entries", views.TimeEntryViewSet, basename="time-entry")
router.register("invoices", views.InvoiceViewSet, basename="invoice")

urlpatterns = [
    path("invoices/<int:pk>/pdf/", views.invoice_pdf, name="invoice-pdf"),
    path("reports/utilization/", views.utilization, name="utilization-report"),
] + router.urls
