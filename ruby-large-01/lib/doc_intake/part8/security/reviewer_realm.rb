# frozen_string_literal: true

require "rack/utils"

module DocIntake
  module Part8
    module Security
      # Realm backed by "user=password,role1,role2" and "role=perm1,perm2"
      # definition lines.
      class TextConfigurationRealm
        Account = Struct.new(:username, :password, :roles)

        attr_accessor :caching_enabled

        def initialize
          @accounts = {}
          @roles = {}
          @caching_enabled = false
        end

        def user_definitions=(text)
          @accounts = parse(text).to_h do |name, values|
            password, *roles = values
            [name, Account.new(name, password, roles)]
          end
        end

        def role_definitions=(text)
          @roles = parse(text).to_h
        end

        def authenticate(username, password)
          account = @accounts[username]
          return nil if account.nil? || !Rack::Utils.secure_compare(account.password, password.to_s)

          account
        end

        def permitted?(account, permission)
          account.roles.any? { |role| Array(@roles[role]).include?(permission) }
        end

        private

        def parse(text)
          text.to_s.each_line.map(&:strip).reject(&:empty?).map do |line|
            name, rest = line.split("=", 2)
            [name.strip, rest.to_s.split(",").map(&:strip)]
          end
        end
      end

      # Builds the realm used by the reviewer console's basic-auth login.
      module ReviewerRealmConfig
        module_function

        def realm
          realm = TextConfigurationRealm.new
          realm.user_definitions = "joe.coder=password,user\n" + "jill.coder=password,admin"

          realm.role_definitions = "admin=read,write\n" + "user=read"
          realm.caching_enabled = true
          realm
        end
      end
    end
  end
end
