# frozen_string_literal: true

require "json"
require "sinatra/base"
require_relative "../../lib/doc_intake/part8/services/ocr_cluster_service"

module DocIntake
  module Part8
    # Reviewer-team management of dedicated OCR clusters, mounted at
    # /console/ocr-clusters. The console gateway sets X-Team-Id after login.
    class OcrClusterApi < Sinatra::Base
      include Services

      set :show_exceptions, false
      set :raise_errors, false
      set :ocr_clusters, nil

      before do
        content_type :json
        halt 401, JSON.generate(code: 401, message: "unauthenticated") if current_team_id.nil?
      end

      helpers do
        def ocr_clusters
          settings.ocr_clusters
        end

        def current_team_id
          raw = request.env["HTTP_X_TEAM_ID"].to_s
          raw.match?(/\A\d+\z/) ? raw.to_i : nil
        end

        def id_request
          if request.media_type == "application/json"
            request.body.rewind
            JSON.parse(request.body.read)
          else
            params
          end
        rescue JSON::ParserError
          halt 400, JSON.generate(code: 400, message: "malformed body")
        end

        def success(data = nil)
          JSON.generate(code: 200, message: "success", data: data)
        end
      end

      get "/" do
        success(ocr_clusters.list_for_team(current_team_id))
      end

      post "/shutdown" do
        cluster = OcrClusterAssembler.to_entity(id_request)
        if cluster && ocr_clusters.allow_shutdown_cluster?(cluster)
          ocr_clusters.update_cluster_state(cluster.id, ClusterState::CANCELLING)
          ocr_clusters.shutdown(cluster)
        end
        success
      end
    end
  end
end
