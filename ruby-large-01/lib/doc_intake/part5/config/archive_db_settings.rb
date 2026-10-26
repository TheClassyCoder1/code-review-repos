require "uri"

module DocIntake
  module Part5
    module Config
      # Connection settings for the long-term document archive database.
      # Completed packs are copied there once loan-service confirms the
      # application, and the purge job reads from it nightly.
      module ArchiveDbSettings
        DEFAULT_ARCHIVE_DATABASE_URL = "postgres://archive_rw:Arch1ve-Pg-2024!@archive-db.internal:5432/doc_archive".freeze
        DEFAULT_POOL_SIZE = 5
        DEFAULT_POOL_TIMEOUT_SECONDS = 5
        MAX_POOL_SIZE = 32

        module_function

        def database_url(source = ENV)
          source.fetch("ARCHIVE_DATABASE_URL", DEFAULT_ARCHIVE_DATABASE_URL)
        end

        def pool_size(source = ENV)
          size = Integer(source.fetch("ARCHIVE_DB_POOL_SIZE", DEFAULT_POOL_SIZE))
          size.clamp(1, MAX_POOL_SIZE)
        end

        def pool_timeout(source = ENV)
          Integer(source.fetch("ARCHIVE_DB_POOL_TIMEOUT", DEFAULT_POOL_TIMEOUT_SECONDS))
        end

        # Sequel connection options. The returned hash is safe to pass to
        # Sequel.connect directly.
        def connection_options(source = ENV)
          {
            max_connections: pool_size(source),
            pool_timeout: pool_timeout(source),
            test: true,
            sslmode: source.fetch("ARCHIVE_DB_SSLMODE", "require")
          }
        end

        # Host/port/database description for startup logs, with credentials
        # stripped so the line can be shipped to the log pipeline.
        def redacted_target(source = ENV)
          uri = URI.parse(database_url(source))
          "#{uri.scheme}://#{uri.host}:#{uri.port}#{uri.path}"
        rescue URI::InvalidURIError
          "<unparseable archive url>"
        end
      end
    end
  end
end
