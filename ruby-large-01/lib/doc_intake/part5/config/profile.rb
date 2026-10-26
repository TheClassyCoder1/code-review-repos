module DocIntake
  module Part5
    module Config
      # Resolves which deployment profile the process runs under. The profile
      # decides whether test-only conveniences (dummy keys, sandbox endpoints)
      # are allowed at all. Anything that is not explicitly "test" or
      # "development" is treated as a deployed environment.
      class Profile
        class ProfileError < StandardError; end

        TEST_PROFILES = %w[test].freeze
        LOCAL_PROFILES = %w[test development].freeze
        DEPLOYED_PROFILES = %w[production staging].freeze

        attr_reader :name

        def initialize(source = ENV)
          raw = source["DOC_INTAKE_PROFILE"] || source["RACK_ENV"] || "production"
          @name = raw.to_s.strip.downcase
          @name = "production" if @name.empty?
        end

        def test?
          TEST_PROFILES.include?(name)
        end

        def local?
          LOCAL_PROFILES.include?(name)
        end

        def deployed?
          !local?
        end

        # Returns +dummy+ only when running under the test profile. In every
        # other profile a missing variable is a configuration error, so a
        # dummy value can never reach a deployed process.
        def test_only_default(variable, dummy)
          return dummy if test?

          raise ProfileError,
                "#{variable} must be set outside the test profile (profile=#{name})"
        end
      end
    end
  end
end
