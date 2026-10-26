require "rack/builder"

module DocIntake
  module Part2
    # Assembles the review & delivery Rack application. Ordering matters:
    # ReturnToSanitizer and PackOwnershipGate must wrap PackPortal so every
    # pack route sees a sanitised return_to and an authorised pack id.
    module RackStack
      module_function

      def build(db:, submissions:, downloads_repository:, logger: Part2.logger)
        PackPortal.set :review_repository, PackReviewRepository.new(db)
        PackPortal.set :search_repository, PackSearchRepository.new(db)
        PackPortal.set :consent_redirector, ConsentRedirector.new(db, logger: logger)
        PackPortal.set :submissions, submissions
        PackPortal.set :downloads_repository, downloads_repository

        Rack::Builder.new do
          map "/logout" do
            run LogoutResponder.new(logger: logger)
          end

          map "/" do
            use ReturnToSanitizer
            use PackOwnershipGate, db: db, logger: logger
            run PackPortal
          end
        end.to_app
      end
    end
  end
end
