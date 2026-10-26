# Queue worker for hardship analytics, contact exports and the collections cycle.
#   docker build -f deploy/docker/worker.Dockerfile -t borrower-documents-worker:<git-sha> .

FROM composer:2.7.7 AS vendor
WORKDIR /app
COPY composer.json ./
RUN composer install --no-dev --no-scripts --no-autoloader --prefer-dist --no-interaction
COPY . .
RUN composer dump-autoload --optimize --classmap-authoritative --no-dev

FROM php:8.3.12-cli-alpine3.20
RUN apk add --no-cache libzip icu-libs ghostscript poppler-utils \
 && apk add --no-cache --virtual .build-deps $PHPIZE_DEPS libzip-dev icu-dev \
 && docker-php-ext-install zip intl pcntl pdo_mysql \
 && apk del .build-deps

WORKDIR /srv/worker
COPY --from=vendor /app /srv/worker
RUN mkdir -p storage/framework/cache storage/logs

# Exit after an hour so the orchestrator recycles the process and memory stays flat.
CMD ["php", "artisan", "queue:work", "--queue=hardship,contact,default", "--max-time=3600", "--tries=3"]
