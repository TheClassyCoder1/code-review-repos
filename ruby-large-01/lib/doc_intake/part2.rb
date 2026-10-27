require "logger"

module DocIntake
  # Pack review & delivery: the reviewer workspace, borrower pack portal,
  # bureau/fraud integrations and archive hand-off to loan-service.
  module Part2
    class << self
      attr_writer :logger

      def logger
        @logger ||= Logger.new($stdout, progname: "doc-intake.review")
      end
    end
  end
end

require_relative "part2/support/errors"
require_relative "part2/support/html"
require_relative "part2/storage/quarantine_paths"
require_relative "part2/storage/download_location"
require_relative "part2/services/virus_scan_runner"
require_relative "part2/services/thumbnail_renderer"
require_relative "part2/jobs/thumbnail_job"
require_relative "part2/policies/callback_url_policy"
require_relative "part2/services/callback_notifier"
require_relative "part2/integrations/bureau_transport"
require_relative "part2/integrations/bureau_transport_factory"
require_relative "part2/integrations/fraud_signal_client"
require_relative "part2/auth/access_code_store"
require_relative "part2/auth/access_code_issuer"
require_relative "part2/drafts/signed_envelope"
require_relative "part2/drafts/draft_codec"
require_relative "part2/drafts/draft_cache"
require_relative "part2/services/upload_fingerprint"
require_relative "part2/services/duplicate_detector"
require_relative "part2/notes/note_formatter"
require_relative "part2/views/reviewer_notes_view"
require_relative "part2/presenters/pack_summary_presenter"
require_relative "part2/views/summary_card"
require_relative "part2/api/sort_params"
require_relative "part2/repositories/pack_search_repository"
require_relative "part2/repositories/pack_review_repository"
require_relative "part2/repositories/upload_quota_repository"
require_relative "part2/consent/consent_state"
require_relative "part2/consent/consent_redirector"
require_relative "part2/session/post_logout_targets"
require_relative "part2/session/logout_responder"
require_relative "part2/middleware/pack_ownership_gate"
require_relative "part2/middleware/return_to_sanitizer"
require_relative "part2/services/pack_archiver"
require_relative "part2/services/affordability_projection"
require_relative "part2/services/document_lookup"
require_relative "part2/services/checksum_manifest"
require_relative "part2/services/ocr_result_cache"
require_relative "part2/parsers/statement_line_parser"
require_relative "part2/parsers/marker_splitter"
require_relative "part2/web/pack_portal"
require_relative "part2/web/rack_stack"
