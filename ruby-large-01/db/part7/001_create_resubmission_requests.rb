Sequel.migration do
  up do
    create_table?(:resubmission_requests) do
      primary_key :id
      Integer :application_id, null: false
      Integer :pack_id, null: false
      String :document_kind, size: 40, null: false
      String :reason, size: 40, null: false, default: "other"
      String :note, size: 500
      String :status, size: 20, null: false, default: "open"
      String :requested_by, size: 80
      DateTime :requested_at, null: false
      DateTime :due_at
      Integer :fulfilled_document_id
      DateTime :fulfilled_at
      Integer :reminder_count, null: false, default: 0
      DateTime :last_reminded_at

      index %i[application_id status]
      index %i[pack_id document_kind]
      index :due_at
    end

    alter_table(:pack_documents) do
      add_column :superseded_at, DateTime
      add_column :superseded_by, Integer
      add_index %i[pack_id kind superseded_at]
    end
  end

  down do
    alter_table(:pack_documents) do
      drop_index %i[pack_id kind superseded_at]
      drop_column :superseded_by
      drop_column :superseded_at
    end

    drop_table?(:resubmission_requests)
  end
end
