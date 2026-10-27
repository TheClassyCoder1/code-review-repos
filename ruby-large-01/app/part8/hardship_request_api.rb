# frozen_string_literal: true

require "json"
require "sinatra/base"
require "time"
require_relative "../../lib/doc_intake/part8/services/hardship_request_service"

module DocIntake
  module Part8
    # Borrower-facing hardship requests, mounted at /portal/hardship.
    # Permissions are granted to the portal role at login and stored in the
    # rack session alongside the borrower id.
    class HardshipRequestApi < Sinatra::Base
      set :show_exceptions, false
      set :raise_errors, false
      set :hardship_requests, nil

      before do
        content_type :json
      end

      helpers do
        def hardship_requests
          settings.hardship_requests
        end

        def current_borrower_id
          session["borrower_id"]
        end

        def require_permission!(permission)
          granted = Array(session["permissions"])
          halt 403, JSON.generate(code: 403, msg: "forbidden") unless granted.include?(permission)
        end

        def success(data)
          JSON.generate(code: 0, msg: "", data: data)
        end
      end

      get "/page" do
        require_permission!("hardship:request:query")
        page = Integer(params.fetch("pageNo", "1"))
        size = Integer(params.fetch("pageSize", "10"))
        success(hardship_requests.page_for_borrower(current_borrower_id, page: page, size: size).map(&:to_api))
      end

      # Get one hardship request by id.
      get "/get" do
        require_permission!("hardship:request:query")
        id = Integer(params.fetch("id"))
        hardship_request = hardship_requests.get_request(id)
        success(hardship_request&.to_api)
      end
    end
  end
end
