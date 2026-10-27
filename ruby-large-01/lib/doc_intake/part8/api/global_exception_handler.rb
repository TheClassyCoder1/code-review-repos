# frozen_string_literal: true

require "json"

module DocIntake
  module Part8
    module Api
      class NotLoginError < StandardError; end

      class NotRoleError < StandardError
        attr_reader :role

        def initialize(role)
          @role = role
          super("missing role #{role}")
        end
      end

      class NotPermissionError < StandardError
        attr_reader :permission

        def initialize(permission)
          @permission = permission
          super("missing permission #{permission}")
        end
      end

      class DisableServiceError < StandardError
        attr_reader :service, :level, :disable_time

        def initialize(service, level, disable_time)
          @service = service
          @level = level
          @disable_time = disable_time
          super("service #{service} disabled")
        end
      end

      # Envelope the reviewer console JS expects for every response.
      class AjaxJson
        attr_reader :code, :msg, :data

        def self.not_login
          new(401, "not logged in")
        end

        def self.not_jur(msg)
          new(403, msg)
        end

        def self.error(msg)
          new(500, msg)
        end

        def initialize(code, msg, data = nil)
          @code = code
          @msg = msg
          @data = data
        end

        def set_msg(msg)
          @msg = msg
          self
        end

        def to_json(*args)
          { code: code, msg: msg, data: data }.to_json(*args)
        end
      end

      # Sinatra extension: turns any exception raised by a console route into
      # an AjaxJson body.
      module GlobalExceptionHandler
        def self.registered(app)
          app.error(StandardError) do
            e = env["sinatra.error"]
            aj = GlobalExceptionHandler.handler_exception(e)
            status(aj.code)
            content_type :json
            aj.to_json
          end
        end

        def self.handler_exception(e)
          # print the stack trace for debugging
          puts "global exception---------------"
          puts e.full_message

          # different exceptions map to different status codes
          if e.is_a?(NotLoginError)
            aj = AjaxJson.not_login.set_msg(e.message)
          elsif e.is_a?(NotRoleError)
            aj = AjaxJson.not_jur("no such role: #{e.role}")
          elsif e.is_a?(NotPermissionError)
            aj = AjaxJson.not_jur("no such permission: #{e.permission}")
          elsif e.is_a?(DisableServiceError)
            aj = AjaxJson.not_jur("account service #{e.service} is disabled (level=#{e.level}): unlocks in #{e.disable_time} seconds")
          else # generic exception: 500 + exception message
            aj = AjaxJson.error(e.message)
          end

          # returned to the front end
          aj
        end
      end
    end
  end
end
