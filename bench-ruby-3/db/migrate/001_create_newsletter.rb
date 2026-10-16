Sequel.migration do
  change do
    create_table(:accounts) do
      primary_key :id
      String :email, null: false, unique: true
      String :name, null: false
      String :password_digest
      String :api_token_digest, index: true
      DateTime :api_token_rotated_at
      TrueClass :admin, null: false, default: false
      DateTime :disabled_at
      String :disabled_reason
      DateTime :last_login_at
      String :last_login_ip
      DateTime :created_at
      DateTime :updated_at
    end

    create_table(:publications) do
      primary_key :id
      foreign_key :account_id, :accounts, null: false, on_delete: :cascade
      String :name, null: false
      String :slug, null: false, unique: true
      String :sender_name
      String :reply_to
      String :footer_html, text: true
      String :postal_address
      String :feed_url
      DateTime :feed_checked_at
      Integer :feed_item_count
      TrueClass :double_opt_in, null: false, default: true
      DateTime :created_at
      DateTime :updated_at
    end

    create_table(:subscribers) do
      primary_key :id
      foreign_key :publication_id, :publications, null: false, on_delete: :cascade
      String :email, null: false
      String :first_name
      String :status, null: false, default: "pending"
      column :tags, "text[]", default: Sequel.lit("'{}'")
      String :confirmation_token, index: true
      DateTime :confirmation_sent_at
      DateTime :confirmed_at
      DateTime :unsubscribed_at
      String :unsubscribe_reason
      DateTime :created_at
      DateTime :updated_at
      unique [:publication_id, :email]
    end

    create_table(:campaigns) do
      primary_key :id
      foreign_key :publication_id, :publications, null: false, on_delete: :cascade
      String :subject, null: false
      String :body_template, text: true, null: false
      String :layout, null: false, default: "minimal"
      String :state, null: false, default: "draft"
      String :preview_slug
      DateTime :preview_generated_at
      DateTime :scheduled_at
      Integer :sent_count, null: false, default: 0
      Integer :open_count, null: false, default: 0
      Integer :click_count, null: false, default: 0
      DateTime :created_at
      DateTime :updated_at
    end

    create_table(:campaign_events) do
      primary_key :id
      Integer :campaign_id, index: true
      String :kind, null: false
      String :subscriber_email
      String :target
      DateTime :created_at, null: false
    end

    create_table(:outbound_emails) do
      primary_key :id
      String :kind, null: false
      Integer :subscriber_id, null: false
      DateTime :sent_at
      DateTime :created_at, null: false
    end
  end
end
