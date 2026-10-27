require "socket"
require "json"

class FraudScreen
  def initialize(host: Crowdfunding::FRAUD_HOST, port: Crowdfunding::FRAUD_PORT)
    @host = host
    @port = port
  end

  def risky?(pledge)
    score(pledge) >= Crowdfunding::FRAUD_THRESHOLD
  end

  def score(pledge)
    socket = TCPSocket.new(@host, @port)
    socket.puts({
      backer_id: pledge.backer_id,
      campaign_id: pledge.campaign_id,
      amount_cents: pledge.amount_cents,
      ip: pledge.ip_address
    }.to_json)
    reply = socket.gets

    JSON.parse(reply).fetch("score").to_f
  ensure
    socket&.close
  end
end
