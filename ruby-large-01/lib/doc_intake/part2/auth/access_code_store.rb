require "digest"
require "openssl"

module DocIntake
  module Part2
    # Persists borrower pack access codes. Codes are looked up by digest so
    # the plaintext never reaches the database. Codes are written only by
    # AccessCodeIssuer.
    class AccessCodeStore
      TABLE = :pack_access_codes

      def initialize(db)
        @db = db
      end

      def save(pack_id, code, expires_at)
        @db[TABLE].insert(
          pack_id: pack_id,
          code_digest: digest(code),
          expires_at: expires_at,
          created_at: Time.now.utc
        )
      end

      # Returns the pack id the code unlocks, or nil when the code is
      # unknown, expired or already revoked.
      def redeem(code)
        row = @db[TABLE].where(code_digest: digest(code), revoked_at: nil).first
        return nil if row.nil?
        return nil if row[:expires_at] && row[:expires_at] < Time.now.utc

        row[:pack_id]
      end

      def revoke_all(pack_id)
        @db[TABLE].where(pack_id: pack_id, revoked_at: nil).update(revoked_at: Time.now.utc)
      end

      private

      def digest(code)
        Digest::SHA256.hexdigest(code.to_s)
      end
    end
  end
end
