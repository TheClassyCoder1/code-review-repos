require "date"
require "json"
require "net/http"
require "uri"

module DocIntake
  module Part3
    # Pulls a fresh credit bureau report for a pack. Every pull is billed by
    # the bureau and leaves a hard-inquiry footprint, so a pack should be
    # pulled at most once per day no matter how many times underwriting asks.
    class BureauRefresh
      def initialize(db, base_url:, api_key:, clock: -> { Time.now.utc })
        @db = db
        @base = URI(base_url)
        @api_key = api_key
        @clock = clock
      end

      def refresh(pack_id, applicant_ref)
        today = @clock.call.to_date
        existing = @db[:bureau_pulls].where(pack_id: pack_id, pulled_on: today).first
        return existing[:report_ref] if existing

        report_ref = pull(applicant_ref)
        @db[:bureau_pulls].insert(
          pack_id: pack_id,
          pulled_on: today,
          report_ref: report_ref,
          created_at: @clock.call
        )
        Part3.logger.info("bureau pull stored pack=#{pack_id} report=#{report_ref}")
        report_ref
      end

      def pulls_for(pack_id)
        @db[:bureau_pulls].where(pack_id: pack_id).order(Sequel.desc(:pulled_on)).all
      end

      private

      def pull(applicant_ref)
        http = Net::HTTP.new(@base.host, @base.port)
        http.use_ssl = @base.scheme == "https"

        req = Net::HTTP::Post.new("#{@base.path}/reports", "content-type" => "application/json")
        req["x-api-key"] = @api_key
        req.body = JSON.generate(applicant: applicant_ref, product: "consumer-full")

        response = http.start { |conn| conn.request(req) }
        raise Error, "bureau returned #{response.code}" unless response.is_a?(Net::HTTPSuccess)

        JSON.parse(response.body).fetch("report_id")
      end
    end
  end
end
