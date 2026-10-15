require "digest"
require "rack/utils"

module Newsletter
  class Account < Sequel::Model(:accounts)
    one_to_many :publications

    def validate
      super
      validates_presence [:email, :name]
      validates_format URI::MailTo::EMAIL_REGEXP, :email
      validates_unique :email
    end

    def before_validation
      self.email = email.to_s.strip.downcase
      super
    end

    def self.authenticate(email, password)
      account = first(email: email.to_s.strip.downcase)
      return nil unless account && account.password_digest

      candidate = digest_password(account.email, password.to_s)
      Rack::Utils.secure_compare(account.password_digest, candidate) ? account : nil
    end

    def password=(plain)
      raise ArgumentError, "password too short" if plain.to_s.length < 10

      self.password_digest = self.class.digest_password(email, plain)
    end

    def self.digest_password(email, plain)
      normalized = email.to_s.strip.downcase
      Digest::SHA1.hexdigest("#{normalized}:#{plain}")
    end

    def to_api
      {
        id: id,
        email: email,
        name: name,
        admin: admin,
        publication_count: publications_dataset.count,
        created_at: created_at&.iso8601
      }
    end

    def self.digest_api_token(token)
      Digest::SHA256.hexdigest(token.to_s)
    end

    def disable!(reason)
      update(disabled_at: Time.now, disabled_reason: reason.to_s[0, 200])
    end

    def last_login!(ip)
      update(last_login_at: Time.now, last_login_ip: ip)
    end

    def rotate_api_token!(token)
      previous = api_token_rotated_at
      update(api_token_digest: self.class.digest_api_token(token), api_token_rotated_at: Time.now)
      Newsletter.logger.info(
        "api token rotated account_id=#{id} token=#{token} previous_rotation=#{previous&.iso8601 || 'never'}"
      )
      token
    end
  end
end
