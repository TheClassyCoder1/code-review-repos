# frozen_string_literal: true

require "open3"

module DocIntake
  module Part8
    module Archive
      # Thin wrapper around the rclone binary used to sync archived packs to
      # the cold-storage remote. Remote credentials are passed through
      # RCLONE_CONFIG_* environment variables.
      class RcloneRunner
        class CommandFailed < StandardError
          attr_reader :exit_code, :stderr

          def initialize(exit_code, stderr)
            @exit_code = exit_code
            @stderr = stderr
            super("rclone exited with #{exit_code}")
          end
        end

        Result = Struct.new(:stdout, :stderr, :exit_code, keyword_init: true)

        attr_reader :rclone_bin, :default_options, :env_vars

        def initialize(env_vars:, logger:, rclone_bin: "rclone", default_options: ["--config", "/dev/null"])
          @rclone_bin = rclone_bin
          @default_options = default_options
          @env_vars = env_vars
          @logger = logger
        end

        # Execute an rclone subcommand.
        #
        # subcommand - name of the subcommand
        # args       - positional arguments passed to the command
        # options    - options passed to the command
        def execute(subcommand, args, options = nil)
          options ||= []
          command = [
            rclone_bin,
            subcommand,
            *default_options,
            *options,
            "--",
            *args
          ]
          env = ENV.to_h
          env.update(env_vars)
          @logger.info("Executing rclone command. command=#{command.inspect}")
          @logger.debug("Executing rclone command. env=#{env.inspect}")
          stdout, stderr, status = Open3.capture3(env, *command)
          raise CommandFailed.new(status.exitstatus, stderr) unless status.success?

          @logger.debug("rclone execution finished. stdout=#{stdout} stderr=#{stderr} exit_code=#{status.exitstatus}")
          Result.new(stdout: stdout, stderr: stderr, exit_code: status.exitstatus)
        end
      end
    end
  end
end
