# frozen_string_literal: true

module DocIntake
  module Part8
    module Session
      # Thin view over the portal's rack session for the logged-in borrower.
      class PortalSessionAuth
        LOGIN_ID_KEY = "borrower_id"
        TOKEN_KEY = "portal_token"

        def initialize(rack_session)
          @session = rack_session
        end

        def logged_in?
          !@session[LOGIN_ID_KEY].nil? && !@session[TOKEN_KEY].to_s.empty?
        end

        def login_id_as_int
          Integer(@session[LOGIN_ID_KEY])
        end

        def token_value
          @session[TOKEN_KEY]
        end
      end

      # Backs GET /portal/session/status, polled by the portal SPA.
      class PortalLoginStatus
        def initialize(session_auth)
          @session_auth = session_auth
        end

        def logged_in?
          if @session_auth.logged_in?
            puts "id:#{@session_auth.login_id_as_int}"
            puts "token: #{@session_auth.token_value}"
          else
            puts "not logged in"
          end

          @session_auth.logged_in?
        end
      end
    end
  end
end
