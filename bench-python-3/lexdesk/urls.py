from django.urls import include, path

urlpatterns = [
    path("api/accounts/", include("accounts.urls")),
    path("api/cases/", include("cases.urls")),
    path("api/clients/", include("clients.urls")),
    path("api/documents/", include("documents.urls")),
    path("api/billing/", include("billing.urls")),
]
