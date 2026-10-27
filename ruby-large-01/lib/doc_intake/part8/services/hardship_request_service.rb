# frozen_string_literal: true

require "sequel"
require "time"

module DocIntake
  module Part8
    module Services
      HardshipRequest = Struct.new(:id, :borrower_id, :reason, :start_date, :end_date, :status, :created_at,
                                   keyword_init: true) do
        def to_api
          {
            id: id,
            reason: reason,
            startDate: start_date&.to_s,
            endDate: end_date&.to_s,
            status: status,
            createTime: created_at&.utc&.iso8601
          }
        end
      end

      # Payment-hardship requests borrowers file through the portal.
      class HardshipRequestService
        def initialize(db)
          @requests = db[:hardship_requests]
        end

        def get_request(id)
          row = @requests.where(id: id).first
          row && HardshipRequest.new(**row.slice(*HardshipRequest.members))
        end

        def page_for_borrower(borrower_id, page:, size:)
          @requests.where(borrower_id: borrower_id)
                   .order(Sequel.desc(:created_at))
                   .limit(size, (page - 1) * size)
                   .all
                   .map { |row| HardshipRequest.new(**row.slice(*HardshipRequest.members)) }
        end
      end
    end
  end
end
