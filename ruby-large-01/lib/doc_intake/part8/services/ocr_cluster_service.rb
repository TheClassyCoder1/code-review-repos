# frozen_string_literal: true

module DocIntake
  module Part8
    module Services
      module ClusterState
        RUNNING = "running"
        CANCELLING = "cancelling"
        STOPPED = "stopped"
      end

      OcrCluster = Struct.new(:id, :team_id, :state, :endpoint, keyword_init: true)

      # Converts API requests into cluster entities.
      module OcrClusterAssembler
        module_function

        def to_entity(id_request)
          id = id_request["id"]
          return nil if id.nil? || id.to_s !~ /\A\d+\z/

          OcrCluster.new(id: Integer(id))
        end
      end

      # Dedicated OCR worker clusters that teams spin up for large backlogs.
      class OcrClusterService
        def initialize(db:, controller:, logger:)
          @clusters = db[:ocr_clusters]
          @controller = controller
          @logger = logger
        end

        def list_for_team(team_id)
          @clusters.where(team_id: team_id).order(:id).all
        end

        def allow_shutdown_cluster?(cluster)
          row = @clusters.where(id: cluster.id).first
          !row.nil? && row[:state] == ClusterState::RUNNING
        end

        def update_cluster_state(cluster_id, state)
          @clusters.where(id: cluster_id).update(state: state)
        end

        def shutdown(cluster)
          row = @clusters.where(id: cluster.id).first
          @controller.stop(row[:endpoint])
          update_cluster_state(cluster.id, ClusterState::STOPPED)
          @logger.info("ocr cluster stopped cluster=#{cluster.id}")
        end
      end
    end
  end
end
