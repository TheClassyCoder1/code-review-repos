from django.urls import path
from ninja import NinjaAPI

from opsdesk.backoffice import accounts, dashboard, helpdesk_views, i18n, plus_api

api = NinjaAPI(urls_namespace="plus")
api.add_router("/plus/collection", plus_api.router)

urlpatterns = [
    path("admin/login/", accounts.login),
    path("oidc/logout/", accounts.oidc_logout),
    path("i18n/translations/", i18n.translations_view),
    path("helpdesk/tickets/<int:ticket_id>/", helpdesk_views.view_ticket, name="helpdesk_view"),
    path("helpdesk/tickets/<int:ticket_id>/hold/", helpdesk_views.hold_ticket, name="helpdesk_hold"),
    path("dashboard/", dashboard.DashboardView.as_view()),
    path("api/", api.urls),
]
