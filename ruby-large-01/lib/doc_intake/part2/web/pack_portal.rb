require "json"
require "sinatra/base"

module DocIntake
  module Part2
    # Borrower and reviewer pack pages. Mounted by Part2::RackStack behind
    # PackOwnershipGate and ReturnToSanitizer; collaborators are configured
    # through `set` by the stack builder.
    class PackPortal < Sinatra::Base
      set :show_exceptions, false
      set :raise_errors, false

      helpers do
        def reviews
          settings.review_repository
        end

        def download_target
          DownloadLocation.for(params["file"])
        end

        def html(body)
          content_type "text/html; charset=utf-8"
          body
        end
      end

      error NotFound do
        halt 404, { "content-type" => "application/json" }, JSON.generate(error: "not_found")
      end

      error InvalidInput do
        halt 422, { "content-type" => "application/json" }, JSON.generate(error: "invalid_input")
      end

      error Forbidden do
        halt 403, { "content-type" => "application/json" }, JSON.generate(error: "forbidden")
      end

      get "/packs/:pack_id" do
        pack = reviews.find(params["pack_id"])
        presenter = PackSummaryPresenter.new(pack, reviews.documents(pack[:id]))
        notes = ReviewerNotesView.new(reviews.notes(pack[:id])).render
        html(SummaryCard.render(presenter) + notes)
      end

      get "/packs/:pack_id/download" do
        settings.downloads_repository.record(params["pack_id"], session_actor)
        redirect download_target, 302
      end

      post "/packs/:pack_id/submit" do
        reviews.find(params["pack_id"])
        settings.submissions.submit(params["pack_id"])
        redirect request.GET["return_to"] || "/portal/packs", 303
      end

      post "/packs/:pack_id/notes" do
        reviewer_id = session_actor
        halt 403 unless env["rack.session"]&.key?("reviewer_id")

        note = reviews.add_note(params["pack_id"], reviewer_id, env["rack.session"]["reviewer_handle"], params["body"])
        content_type :json
        JSON.generate(id: note[:id])
      end

      get "/review/packs" do
        halt 403 unless env["rack.session"]&.key?("reviewer_id")

        sort = SortParams.parse(params["sort"])
        page = settings.search_repository.search(sort: sort, status: params["status"], query: params["q"],
                                                 page: params["page"] || 1)
        cards = page.rows.map { |row| PackSummaryPresenter.new(row, []) }
        html(SummaryCard.render_all(cards))
      end

      post "/consent/complete" do
        borrower_id = env["rack.session"] && env["rack.session"]["borrower_id"]
        halt 401 if borrower_id.nil?

        status, headers, body = settings.consent_redirector.complete(params, borrower_id)
        halt status, headers, body
      end

      private

      def session_actor
        session_hash = env["rack.session"] || {}
        session_hash["reviewer_id"] || session_hash["borrower_id"]
      end
    end
  end
end
