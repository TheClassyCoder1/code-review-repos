require "net/http"

Rails.application.config.to_prepare do
  gateway = URI(ENV.fetch("PAYMENT_GATEWAY_URL", "https://api.gateway.example"))

  PaymentGatewayClient.connection_factory = lambda do
    Net::HTTP.new(gateway.host, gateway.port).tap do |http|
      http.use_ssl = gateway.scheme == "https"
      http.open_timeout = 3
      http.read_timeout = 10
      http.write_timeout = 10
    end
  end
end
