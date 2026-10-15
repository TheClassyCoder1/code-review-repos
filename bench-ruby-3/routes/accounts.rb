require "securerandom"

module Newsletter
  class App
    get "/login" do
      content_type :html
      "<form method=\"post\" action=\"/login\"><input name=\"email\"><input name=\"password\" type=\"password\"></form>"
    end

    post "/login" do
      account = Account.authenticate(params[:email], params[:password])
      halt 401, render_json({ error: "invalid email or password" }) unless account

      session.clear
      session[:account_id] = account.id
      account.last_login!(request.ip)
      redirect(params[:return_to] || "/publications")
    end

    post "/logout" do
      session.clear
      redirect "/login"
    end

    post "/account/api_token" do
      require_account!
      token = SecureRandom.urlsafe_base64(32)
      current_account.rotate_api_token!(token)
      render_json({ token: token, rotated_at: current_account.api_token_rotated_at.iso8601 })
    end

    get "/account" do
      require_account!
      render_json(current_account.to_api)
    end
  end
end
