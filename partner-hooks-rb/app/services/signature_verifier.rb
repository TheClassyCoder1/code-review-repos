require "openssl"

module SignatureVerifier
  module_function

  def sign(payload, secret)
    "sha256=#{OpenSSL::HMAC.hexdigest('SHA256', secret, payload)}"
  end

  def valid?(payload, secret, provided)
    return false if provided.blank? || secret.blank?

    ActiveSupport::SecurityUtils.secure_compare(sign(payload, secret), provided)
  end
end
