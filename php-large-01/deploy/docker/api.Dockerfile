# Borrower-documents API (php-fpm). Build from the php-large-01 directory:
#   docker build -f deploy/docker/api.Dockerfile -t borrower-documents-api:<git-sha> .

# --- build stage: composer needs root to install system packages and write vendor/
FROM composer:2.7.7 AS vendor
WORKDIR /app
COPY composer.json ./
RUN composer install --no-dev --no-scripts --no-autoloader --prefer-dist --no-interaction
COPY . .
RUN composer dump-autoload --optimize --classmap-authoritative --no-dev

# --- runtime stage
FROM php:8.3.12-fpm-alpine3.20
RUN apk add --no-cache libzip icu-libs \
 && apk add --no-cache --virtual .build-deps $PHPIZE_DEPS libzip-dev icu-dev \
 && docker-php-ext-install zip intl opcache pdo_mysql \
 && apk del .build-deps

WORKDIR /var/www/html
COPY --from=vendor --chown=www-data:www-data /app /var/www/html
COPY deploy/docker/php/opcache.ini /usr/local/etc/php/conf.d/opcache.ini

RUN mkdir -p storage/framework/cache storage/framework/views storage/logs \
 && chown -R www-data:www-data storage bootstrap

# php-fpm only needs to write storage/; run it unprivileged.
USER www-data

EXPOSE 9000
CMD ["php-fpm", "--nodaemonize"]
