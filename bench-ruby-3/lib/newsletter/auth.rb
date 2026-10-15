require "json"

module Newsletter
  module Auth
    def current_account
      return @current_account if defined?(@current_account)

      account = session[:account_id] && Account[session[:account_id]]
      account ||= api_account
      @current_account = account&.disabled_at ? nil : account
    end

    def api_account
      header = request.env["HTTP_AUTHORIZATION"].to_s
      return nil unless header.start_with?("Bearer ")

      Account.first(api_token_digest: Account.digest_api_token(header.delete_prefix("Bearer ")))
    end

    def require_account!
      halt 401, render_json({ error: "authentication required" }) unless current_account
    end

    def sanitize_return_to!
      target = params["return_to"].to_s
      local = target.start_with?("/") && !target.start_with?("//") && !target.include?("\\")
      params["return_to"] = nil unless local
    end

    def owned_publication(id)
      current_account.publications_dataset.with_pk!(Integer(id))
    end

    def owned_campaign(id)
      Campaign.where(id: Integer(id), publication_id: current_account.publications_dataset.select(:id)).first!
    end

    def render_json(payload)
      content_type :json
      JSON.generate(payload)
    end
  end
end
