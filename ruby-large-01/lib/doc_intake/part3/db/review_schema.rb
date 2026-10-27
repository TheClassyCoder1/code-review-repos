module DocIntake
  module Part3
    # Schema for the review workflow tables. Applied by the migration runner
    # in config/boot.rb (`Part3::ReviewSchema.apply!(DB)`); safe to re-run.
    module ReviewSchema
      module_function

      def apply!(db)
        db.create_table?(:reviewers) do
          primary_key :id
          String :team, null: false
          Integer :open_tasks, null: false, default: 0
          Integer :max_open_tasks, null: false, default: 25
          TrueClass :active, null: false, default: true
        end

        db.create_table?(:review_tasks) do
          primary_key :id
          # A task cannot outlive the pack it reviews: deleting a pack removes
          # its tasks in the same statement, and a task can never point at a
          # pack id that does not exist.
          foreign_key :pack_id, :document_packs, null: false, on_delete: :cascade
          foreign_key :assignee_id, :reviewers, null: true, on_delete: :set_null
          String :state, null: false, default: "queued"
          String :decision
          DateTime :claimed_at
          DateTime :created_at, null: false
          index %i[state created_at]
        end

        db.create_table?(:pack_event_receipts) do
          primary_key :id
          String :idempotency_key, null: false
          Integer :pack_id, null: false
          String :event_type, null: false
          String :payload_digest, null: false
          DateTime :received_at, null: false
          # The key is derived from (pack_id, event_type, payload_digest), so
          # a second row with the same key is the same event delivered twice.
          index :idempotency_key, unique: true
        end

        db.create_table?(:bureau_pulls) do
          primary_key :id
          Integer :pack_id, null: false
          Date :pulled_on, null: false
          String :report_ref
          DateTime :created_at, null: false
          index %i[pack_id pulled_on]
        end

        db.create_table?(:pack_outbox) do
          primary_key :id
          Integer :pack_id, null: false
          String :topic, null: false
          String :payload, text: true, null: false
          Integer :attempts, null: false, default: 0
          DateTime :sent_at
          DateTime :created_at, null: false
          index %i[sent_at id]
        end

        db.create_table?(:pack_page_ledgers) do
          primary_key :id
          Integer :pack_id, null: false, unique: true
          Integer :pages_expected, null: false, default: 0
          Integer :pages_received, null: false, default: 0
          DateTime :updated_at
        end

        db.create_table?(:pack_summaries) do
          primary_key :id
          Integer :pack_id, null: false, unique: true
          Integer :document_count, null: false, default: 0
          Bignum :total_bytes, null: false, default: 0
          DateTime :updated_at
        end

        db.create_table?(:document_tags) do
          primary_key :id
          Integer :pack_id, null: false
          String :tag, null: false
          String :source, null: false, default: "reviewer"
          DateTime :created_at, null: false
          index %i[pack_id tag], unique: true
        end

        db.create_table?(:ocr_pages) do
          primary_key :id
          Integer :pack_id, null: false
          Integer :page_number, null: false
          String :text, text: true
          Float :confidence
          index %i[pack_id page_number], unique: true
        end

        db.create_table?(:statement_lines) do
          primary_key :id
          Integer :pack_id, null: false
          Date :posted_on, null: false
          String :description, null: false
          BigDecimal :amount, size: [14, 2], null: false
          index %i[pack_id posted_on]
        end

        db.create_table?(:webhook_deliveries) do
          primary_key :id
          String :target_url, null: false
          String :payload, text: true, null: false
          Integer :attempts, null: false, default: 0
          String :status, null: false, default: "pending"
          DateTime :delivered_at
          DateTime :created_at, null: false
        end
      end
    end
  end
end
