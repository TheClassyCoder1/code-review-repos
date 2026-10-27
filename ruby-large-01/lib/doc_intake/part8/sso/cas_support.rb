# frozen_string_literal: true

require "json"
require "net/http"
require "uri"

module DocIntake
  module Part8
    module Sso
      module CasConstants
        ENDPOINT_SERVICE_VALIDATE_V3 = "/p3/serviceValidate"
        SERVICE_TICKET_PREFIX = "ST-"
        FORMAT_XML = "XML"
        FORMAT_JSON = "JSON"
        INVALID_SERVICE = "INVALID_SERVICE"
        INVALID_TICKET = "INVALID_TICKET"
      end

      Authentication = Struct.new(:principal, :attributes, keyword_init: true)
      CasDetails = Struct.new(:service, :adapter, :is_adapter, keyword_init: true) do
        def adapter?
          is_adapter.to_s == "1" || is_adapter == true
        end
      end
      Ticket = Struct.new(:id, :authentication, :cas_details, keyword_init: true)
      ProxyGrantingTicketIou = Struct.new(:id)
      ProxyGrantingTicket = Struct.new(:id, :authentication, :cas_details)

      # Normalises a service URL the way the registered CAS apps are stored.
      module HttpUtils
        module_function

        def request_url(service)
          uri = URI.parse(service.to_s)
          "#{uri.scheme}://#{uri.host}#{":#{uri.port}" unless [80, 443, nil].include?(uri.port)}#{uri.path}"
        rescue URI::InvalidURIError
          service.to_s
        end
      end

      class ServiceResponseBuilder
        Section = Struct.new(:code, :description, :ticket, :proxy, :attributes)

        attr_reader :format

        def initialize(format)
          @format = format.to_s.upcase
          @success = nil
          @failure = nil
        end

        def success
          @success ||= Section.new(nil, nil, nil, nil, {})
        end

        def failure
          @failure ||= Section.new
        end

        def build
          if @failure
            { serviceResponse: { authenticationFailure: { code: @failure.code, description: @failure.description } } }
          else
            { serviceResponse: { authenticationSuccess: success.to_h.compact } }
          end
        end
      end

      class HttpRequestAdapter
        OPEN_TIMEOUT = 3
        READ_TIMEOUT = 5

        def post(url, body)
          uri = URI.parse(url)
          http = Net::HTTP.new(uri.host, uri.port)
          http.use_ssl = uri.scheme == "https"
          http.open_timeout = OPEN_TIMEOUT
          http.read_timeout = READ_TIMEOUT
          request = Net::HTTP::Post.new(uri.request_uri)
          request.body = body.to_s
          http.request(request)
        end
      end

      class HttpResponseAdapter
        def write(response, data, format)
          if format.to_s.upcase == CasConstants::FORMAT_JSON
            response["content-type"] = "application/json"
            response.body = [JSON.generate(data)]
          else
            response["content-type"] = "application/xml"
            response.body = [to_xml(data)]
          end
        end

        private

        def to_xml(value, name = nil)
          inner =
            case value
            when Hash then value.map { |k, v| to_xml(v, k) }.join
            else value.to_s.encode(xml: :text)
            end
          name ? "<cas:#{name}>#{inner}</cas:#{name}>" : inner
        end
      end

      # Base class for per-app attribute adapters configured on a CAS app.
      class AbstractAuthorizeAdapter
        attr_accessor :service_response_builder, :principal, :app

        def generate_info
          service_response_builder.success.attributes[:user] = principal.to_s
        end
      end
    end
  end
end
