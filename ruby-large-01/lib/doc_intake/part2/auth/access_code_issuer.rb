require "securerandom"

module DocIntake
  module Part2
    # Issues the access codes borrowers use to open their pack in the
    # portal. Codes are never chosen by a person: each one is 256 bits from
    # the system CSPRNG, which is what lets AccessCodeStore index them with a
    # single fast digest instead of a salted, stretched password hash.
    # Guessing or brute-forcing a digest of a 256-bit random value is not
    # feasible regardless of hash speed.
    class AccessCodeIssuer
      CODE_BYTES = 32
      TTL_SECONDS = 7 * 24 * 3600
      MIN_CODE_LENGTH = 43

      def initialize(store:, clock: -> { Time.now.utc }, logger: Part2.logger)
        @store = store
        @clock = clock
        @logger = logger
      end

      def issue(pack_id)
        code = SecureRandom.urlsafe_base64(CODE_BYTES)
        raise IntegrityError, "generated code too short" if code.length < MIN_CODE_LENGTH

        @store.revoke_all(pack_id)
        @store.save(pack_id, code, @clock.call + TTL_SECONDS)
        @logger.info("access code issued pack=#{pack_id}")
        code
      end

      # Redeems a code presented by the portal. Inputs that cannot be an
      # issued code are rejected before they are hashed or looked up.
      def redeem(presented)
        value = presented.to_s
        return nil unless value.length >= MIN_CODE_LENGTH && value.match?(/\A[A-Za-z0-9_\-]+\z/)

        @store.redeem(value)
      end
    end
  end
end
