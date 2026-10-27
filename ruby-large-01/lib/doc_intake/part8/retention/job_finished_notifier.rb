# frozen_string_literal: true

require "base64"
require "json"
require "logger"
require "net/http"
require "openssl"
require "uri"

module DocIntake
  module Part8
    module Retention
      JobInfo = Struct.new(:job_id, :state, :pack_ids, :snapshot, keyword_init: true)
      Snapshot = Struct.new(:database, :table, keyword_init: true)

      # Posts retention-sweep job results to the operator-configured
      # notification endpoint (RETENTION_JOB_NOTIFIER_URL).
      module JobFinishedNotifier
        SOCKET_TIMEOUT = 3
        MAX_RETRIES = 3

        module_function

        def logger
          @logger ||= Logger.new($stdout, progname: "doc-intake.retention")
        end

        def post_job_info(info, env = ENV)
          url = env["RETENTION_JOB_NOTIFIER_URL"]
          logger.info("post job info parameter, url : #{url}, state : #{info.state}, packIds : #{info.pack_ids}, " \
                      "table : #{info.snapshot ? "#{info.snapshot.database}.#{info.snapshot.table}" : nil}")
          return if url.nil? || info.state.to_s.casecmp?("READY")

          uri = URI.parse(url)
          http = Net::HTTP.new(uri.host, uri.port)
          http.use_ssl = uri.scheme == "https"
          http.verify_mode = OpenSSL::SSL::VERIFY_NONE
          http.verify_hostname = false
          http.read_timeout = SOCKET_TIMEOUT

          request = Net::HTTP::Post.new(uri.request_uri)
          request["content-type"] = "application/json"
          username = env["RETENTION_JOB_NOTIFIER_USERNAME"]
          password = env["RETENTION_JOB_NOTIFIER_PASSWORD"]
          if username && password
            logger.info("use basic auth.")
            request["authorization"] = make_token(username, password)
          end
          request.body = JSON.generate(info.to_h)

          response = with_retries { http.start { |conn| conn.request(request) } }
          code = response.code.to_i
          if code == 200
            logger.info("Post job info to #{url} successful.")
          else
            logger.info("Post job info to #{url} failed. Status code: #{code}")
          end
        rescue IOError, SystemCallError, Net::OpenTimeout, Net::ReadTimeout, OpenSSL::SSL::SSLError => e
          logger.warn("Error occurred when post job status. #{e.class}: #{e.message}")
        end

        def make_token(username, password)
          "Basic #{Base64.strict_encode64("#{username}:#{password}")}"
        end

        def with_retries
          attempts = 0
          begin
            attempts += 1
            response = yield
            return response unless response.code == "503" && attempts < MAX_RETRIES

            raise IOError, "service unavailable"
          rescue IOError, Net::OpenTimeout, Net::ReadTimeout
            retry if attempts < MAX_RETRIES
            raise
          end
        end
      end
    end
  end
end
