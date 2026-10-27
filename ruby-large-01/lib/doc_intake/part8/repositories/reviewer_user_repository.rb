# frozen_string_literal: true

module DocIntake
  module Part8
    module Repositories
      ReviewerUser = Struct.new(:id, :username, :password, :role, keyword_init: true)

      class ReviewerUserRepository
        def initialize(db)
          @users = db[:reviewer_users]
        end

        def load_by_user_name(username)
          row = @users.where(username: username).first
          row && ReviewerUser.new(**row.slice(*ReviewerUser.members))
        end

        def update(user)
          @users.where(id: user.id).update(password: user.password, role: user.role)
        end
      end
    end
  end
end
