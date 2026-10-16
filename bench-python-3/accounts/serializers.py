from django.conf import settings
from django.utils.http import url_has_allowed_host_and_scheme
from rest_framework import serializers

from .hashers import MIN_PASSWORD_LENGTH, PASSWORD_CLASSES
from .models import ClientPortalAccount


class PortalLoginSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True)


class PortalSignupSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True, min_length=MIN_PASSWORD_LENGTH)
    invite_code = serializers.CharField(max_length=64)
    return_path = serializers.CharField(required=False, default="/portal/")

    def validate_email(self, email):
        normalized = email.strip().lower()
        if ClientPortalAccount.objects.filter(email=normalized).exists():
            raise serializers.ValidationError("An account with this email already exists.")
        return normalized

    def validate_password(self, password):
        missing = [p.pattern for p in PASSWORD_CLASSES if not p.search(password)]
        if len(missing) > 1:
            raise serializers.ValidationError("Password is too weak.")
        return password

    def validate_return_path(self, value):
        if not url_has_allowed_host_and_scheme(value, allowed_hosts=set(settings.ALLOWED_HOSTS), require_https=True):
            raise serializers.ValidationError("Invalid return path.")
        return value
