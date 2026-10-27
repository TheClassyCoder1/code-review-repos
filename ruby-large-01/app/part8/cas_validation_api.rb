# frozen_string_literal: true

require "logger"
require "sinatra/base"
require_relative "../../lib/doc_intake/part8/sso/cas_support"

module DocIntake
  module Part8
    # CAS 3.0 ticket validation for the reviewer console SSO, mounted at /cas.
    class CasValidationApi < Sinatra::Base
      include Sso

      set :show_exceptions, false
      set :raise_errors, false
      set :ticket_services, nil
      set :pgt_services, nil

      LOG = Logger.new($stdout, progname: "doc-intake.cas")

      helpers do
        def http_request_adapter
          @http_request_adapter ||= HttpRequestAdapter.new
        end

        def http_response_adapter
          @http_response_adapter ||= HttpResponseAdapter.new
        end

        # CAS 3.0 ticket validation: resolves the logged-in user from a ticket.
        def service_validate
          ticket = params.fetch("ticket")
          service = params.fetch("service")
          pgt_url = params["pgtUrl"]
          renew = params["renew"]
          format = params.fetch("format", CasConstants::FORMAT_XML)
          LOG.debug("serviceValidate ticket #{ticket} , service #{service} , pgtUrl #{pgt_url} , renew #{renew} , format #{format}")

          stored_ticket = nil
          if ticket.start_with?(CasConstants::SERVICE_TICKET_PREFIX)
            begin
              stored_ticket = settings.ticket_services.consume_ticket(ticket)
            rescue StandardError => e
              warn e.full_message
            end
          end
          builder = ServiceResponseBuilder.new(format)

          if stored_ticket
            cas_detail = stored_ticket.cas_details
            # the service parameter must match the service stored on the ticket
            request_cas_service = HttpUtils.request_url(service)
            if request_cas_service != cas_detail.service
              LOG.debug("Ticket #{ticket} not valid for registered service #{cas_detail.service} , the request service #{request_cas_service}")
              builder.failure.code = CasConstants::INVALID_SERVICE
              builder.failure.description = "Ticket #{ticket} not valid for this service #{request_cas_service}"
              http_response_adapter.write(response, builder.build, format)
            end

            principal = stored_ticket.authentication.principal
            unless pgt_url.to_s.strip.empty?
              proxy_granting_ticket_iou = settings.pgt_services.create_ticket(ProxyGrantingTicketIou.new)

              pgt = ProxyGrantingTicket.new(nil, stored_ticket.authentication, stored_ticket.cas_details)
              proxy_granting_ticket = settings.pgt_services.create_ticket(pgt)

              builder.success.ticket = proxy_granting_ticket_iou
              builder.success.proxy = pgt_url

              http_request_adapter.post("#{pgt_url}?pgtId=#{proxy_granting_ticket}&pgtIou=#{proxy_granting_ticket_iou}", nil)
            end

            if stored_ticket.cas_details.adapter?
              cas_adapter = Object.const_get(stored_ticket.cas_details.adapter).new
              begin
                cas_adapter.service_response_builder = builder
              rescue NoMethodError => e
                LOG.error("setProperty error . #{e.message}")
              end

              cas_adapter.principal = principal
              cas_adapter.app = stored_ticket.cas_details
              cas_adapter.generate_info
            else
              LOG.error("Cas Adapter is not Set . ")
            end
          else
            builder.failure.code = CasConstants::INVALID_TICKET
            builder.failure.description = "Ticket #{ticket} not recognized"
          end

          http_response_adapter.write(response, builder.build, format)
          nil
        end
      end

      get(CasConstants::ENDPOINT_SERVICE_VALIDATE_V3) { service_validate }
      post(CasConstants::ENDPOINT_SERVICE_VALIDATE_V3) { service_validate }
    end
  end
end
