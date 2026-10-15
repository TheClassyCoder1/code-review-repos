require "openssl"
require "base64"
require "rack/utils"

module Newsletter
  module UnsubscribeToken
    module_function

    def signing_key
      @signing_key ||= OpenSSL::KDF.pbkdf2_hmac(
        Newsletter::SECRET_KEY,
        salt: "newsletter-unsubscribe-link-v1",
        iterations: 20_000,
        length: 32,
        hash: "SHA256"
      )
    end

    def generate(subscriber)
      payload = "#{subscriber.id}.#{subscriber.publication_id}"
      signature = OpenSSL::HMAC.hexdigest("SHA256", signing_key, payload)
      Base64.urlsafe_encode64("#{payload}.#{signature}", padding: false)
    end

    def verify(token)
      decoded = Base64.urlsafe_decode64(token.to_s)
      subscriber_id, publication_id, signature = decoded.split(".", 3)
      return nil if signature.nil? || subscriber_id !~ /\A\d+\z/ || publication_id !~ /\A\d+\z/

      expected = OpenSSL::HMAC.hexdigest("SHA256", signing_key, "#{subscriber_id}.#{publication_id}")
      return nil unless Rack::Utils.secure_compare(expected, signature)

      Subscriber.first(id: subscriber_id.to_i, publication_id: publication_id.to_i)
    rescue ArgumentError
      nil
    end
  end
end
