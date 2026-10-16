from django.urls import path

from . import views

urlpatterns = [
    path("portal/login/", views.portal_login, name="portal-login"),
    path("portal/signup/", views.complete_signup, name="portal-signup"),
    path("logout/", views.logout_view, name="logout"),
]
