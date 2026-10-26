# frozen_string_literal: true

Sequel.migration do
  up do
    create_table?(:identity_checks) do
      primary_key :id
      String :check_ref, size: 36, null: false, unique: true
      Integer :application_id, null: false, index: true
      String :document_kind, size: 32, null: false
      String :national_id, size: 32, null: false
      String :issuing_country, size: 2, null: false
      Date :date_of_birth, null: false
      String :status, size: 16, null: false, default: "pending"
      String :failure_reason, size: 64
      DateTime :created_at, null: false
      DateTime :updated_at, null: false
    end

    create_table?(:contact_verifications) do
      primary_key :id
      Integer :application_id, null: false, index: true
      String :channel, size: 8, null: false
      String :code_digest, size: 64, null: false
      Integer :attempts, null: false, default: 0
      DateTime :expires_at, null: false
      DateTime :verified_at
      DateTime :created_at, null: false
    end

    create_table?(:intake_fee_lines) do
      primary_key :id
      Integer :application_id, null: false, index: true
      String :description, size: 120, null: false
      BigDecimal :amount, size: [12, 2], null: false
      Date :charged_on, null: false
    end
  end

  down do
    drop_table?(:intake_fee_lines)
    drop_table?(:contact_verifications)
    drop_table?(:identity_checks)
  end
end
