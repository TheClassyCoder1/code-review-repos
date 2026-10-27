module DocIntake
  # Document pack processing: statement parsing, payslip extraction,
  # affordability, bureau summaries, review queueing, retention, exports
  # and archiving.
  module Part4
  end
end

%w[
  net_http_timeouts
  statement_source
  statement_ledger_parser
  document_classifier
  upload_validator
  upload_quota_ledger
  upload_quota_gate
  payslip_extractor
  affordability_calculator
  bureau_report_fetcher
  bureau_report_summary
  pack_completeness_checker
  review_queue_prioritizer
  retention_policy
  document_event_builder
  document_page_cursor
  decision_export_writer
  decision_export_batch
  pack_archive_stager
  pack_archiver
].each { |name| require_relative "doc_intake/part4/#{name}" }
