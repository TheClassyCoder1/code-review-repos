# frozen_string_literal: true

require "digest"
require "json"
require "logger"
require "net/http"
require "securerandom"
require "uri"

module DocIntake
  module Part8
    module Otp
      # Sends a one-time code by SMS through the gateway's sendcode endpoint
      # and stores it for the contact-confirmation step.
      class SmsOtpProducer
        SERVER_URL = "https://api.sms-gateway.internal.test/sms/sendcode.action"

        def initialize(app_key:, app_secret:, template_id:, otp_token_store:, digits: 6,
                       logger: Logger.new($stdout, progname: "doc-intake.otp"))
          @app_key = app_key
          @app_secret = app_secret
          @template_id = template_id
          @otp_token_store = otp_token_store
          @digits = digits
          @logger = logger
        end

        def produce(user_info)
          mobile = user_info.mobile
          return false if mobile.nil? || mobile.empty?

          begin
            uri = URI.parse(SERVER_URL)
            cur_time = Time.now.to_i.to_s
            # random number
            nonce = format("%06d", SecureRandom.random_number(1_000_000))
            check_sum = build_check_sum(@app_secret, nonce, cur_time)
            @logger.debug("AppKey #{@app_key} ,Nonce #{nonce}, CurTime #{cur_time} ,checkSum #{check_sum}")
            # request headers
            request = Net::HTTP::Post.new(uri.request_uri)
            request["AppKey"] = @app_key
            request["Nonce"] = nonce
            request["CurTime"] = cur_time
            request["CheckSum"] = check_sum
            request["Content-Type"] = "application/x-www-form-urlencoded;charset=utf-8"

            # form body
            request.body = URI.encode_www_form(
              "templateid" => @template_id,
              "mobile" => user_info.mobile,
              "codeLen" => @digits.to_s
            )
            http = Net::HTTP.new(uri.host, uri.port)
            http.use_ssl = true
            response = http.request(request)

            # {"code":200,"msg":"1","obj":"740673"}
            response_string = response.body.to_s.force_encoding("utf-8")
            @logger.debug("responseString #{response_string}")
            sms = JSON.parse(response_string)
            @logger.debug("responseEntity code #{sms['obj']}")
            nonce = sms["obj"].nil? ? nonce : sms["obj"].to_s
            @logger.debug("nonce #{nonce}")
            @otp_token_store.store(user_info, nonce, user_info.mobile, :sms)
            true
          rescue StandardError => e
            @logger.error(" produce code error #{e.class}")
            false
          end
        end

        private

        def build_check_sum(app_secret, nonce, cur_time)
          Digest::SHA1.hexdigest("#{app_secret}#{nonce}#{cur_time}")
        end
      end
    end
  end
end
