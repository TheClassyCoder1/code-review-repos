# document-intake API image.
#
# The build stage needs root to install native toolchains for nokogiri and
# pg; the runtime stage only carries the vendored bundle and runs as an
# unprivileged user.

FROM ruby:3.3.5-slim-bookworm AS build

RUN apt-get update \
 && apt-get install -y --no-install-recommends build-essential libpq-dev libxml2-dev libxslt1-dev \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /src
COPY Gemfile Gemfile.lock ./
RUN bundle config set --local deployment true \
 && bundle config set --local without "development test" \
 && bundle install --jobs 4

COPY . .

FROM ruby:3.3.5-slim-bookworm AS runtime

RUN apt-get update \
 && apt-get install -y --no-install-recommends libpq5 libxml2 libxslt1.1 clamav \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 10001 intake \
 && useradd --system --uid 10001 --gid intake --home-dir /app --shell /usr/sbin/nologin intake

WORKDIR /app
COPY --from=build --chown=intake:intake /src /app

ENV RACK_ENV=production \
    BUNDLE_DEPLOYMENT=true \
    BUNDLE_WITHOUT="development test"

USER 10001:10001

EXPOSE 8120
HEALTHCHECK --interval=30s --timeout=3s CMD ruby -rnet/http -e 'exit Net::HTTP.get_response(URI("http://127.0.0.1:8120/health")).is_a?(Net::HTTPSuccess)'

CMD ["bundle", "exec", "puma", "config.ru", "-p", "8120", "-e", "production"]
