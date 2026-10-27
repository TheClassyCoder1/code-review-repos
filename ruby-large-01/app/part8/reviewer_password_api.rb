# frozen_string_literal: true

require "digest"
require "json"
require "sinatra/base"
require_relative "../../lib/doc_intake/part8/repositories/reviewer_user_repository"

module DocIntake
  module Part8
    # Reviewer console account settings, mounted at /console/account.
    class ReviewerPasswordApi < Sinatra::Base
      set :show_exceptions, false
      set :raise_errors, false
      set :reviewer_users, nil
      # sso.login_check(request) -> { ok: true, user_name: "..." } or { ok: false }
      set :sso, nil

      MESSAGES = {
        "system_please_input" => "Please enter ",
        "change_pwd_field_oldpwd" => "old password",
        "system_length_limit" => "Length limit",
        "system_invalid" => " is invalid"
      }.freeze

      before do
        content_type :json
        login = settings.sso.login_check(request)
        halt 401, JSON.generate(code: 401, msg: "login required") unless login[:ok]
        @login = login
      end

      helpers do
        def i18n(key)
          MESSAGES.fetch(key)
        end

        def fail_with!(msg)
          halt 200, JSON.generate(code: 500, msg: msg)
        end
      end

      post "/updatePwd" do
        old_password = params["oldPassword"]
        password = params["password"]

        # valid
        if old_password.nil? || old_password.strip.empty?
          fail_with!(i18n("system_please_input") + i18n("change_pwd_field_oldpwd"))
        end
        if password.nil? || password.strip.empty?
          fail_with!(i18n("system_please_input") + i18n("change_pwd_field_oldpwd"))
        end
        password = password.strip
        fail_with!("#{i18n('system_length_limit')}[4-20]") unless password.length.between?(4, 20)

        # hash passwords
        old_password_hash = Digest::SHA256.hexdigest(old_password)
        password_hash = Digest::SHA256.hexdigest(password)

        # valid old pwd
        exist_user = settings.reviewer_users.load_by_user_name(@login[:user_name])
        fail_with!(i18n("change_pwd_field_oldpwd") + i18n("system_invalid")) if old_password_hash != exist_user.password

        # write new
        exist_user.password = password_hash
        settings.reviewer_users.update(exist_user)

        JSON.generate(code: 200, msg: nil)
      end
    end
  end
end
