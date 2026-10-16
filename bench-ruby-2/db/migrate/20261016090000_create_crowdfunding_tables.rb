class CreateCrowdfundingTables < ActiveRecord::Migration[7.1]
  def change
    create_table :backers do |t|
      t.string :name, null: false
      t.string :email, null: false
      t.string :session_token
      t.string :payout_account_id
      t.timestamps
    end
    add_index :backers, :email, unique: true
    add_index :backers, :session_token, unique: true

    create_table :campaigns do |t|
      t.references :creator, null: false, foreign_key: { to_table: :backers }
      t.string :title, null: false
      t.text :description
      t.string :status, null: false, default: "draft"
      t.bigint :goal_cents, null: false
      t.bigint :pledged_cents, null: false, default: 0
      t.integer :backers_count, null: false, default: 0
      t.datetime :starts_at
      t.datetime :ends_at
      t.string :archive_path
      t.timestamps
    end
    add_index :campaigns, %i[status created_at]

    create_table :rewards do |t|
      t.references :campaign, null: false, foreign_key: true
      t.string :title, null: false
      t.text :description
      t.bigint :min_cents, null: false
      t.integer :quantity
      t.integer :claimed_count, null: false, default: 0
      t.boolean :featured, null: false, default: false
      t.timestamps
    end

    create_table :pledges do |t|
      t.references :campaign, null: false, foreign_key: true
      t.references :backer, null: false, foreign_key: true
      t.references :reward, foreign_key: true
      t.bigint :amount_cents, null: false
      t.string :status, null: false, default: "pending"
      t.string :gateway_charge_id
      t.string :ip_address
      t.datetime :refunded_at
      t.timestamps
    end
    add_index :pledges, %i[campaign_id backer_id]
    add_index :pledges, :gateway_charge_id

    create_table :payouts do |t|
      t.references :campaign, null: false, foreign_key: true, index: false
      t.string :status, null: false, default: "pending"
      t.decimal :gross_amount, precision: 14, scale: 2, null: false
      t.decimal :fee_amount, precision: 14, scale: 2, null: false
      t.decimal :net_amount, precision: 14, scale: 2, null: false
      t.string :transfer_reference
      t.datetime :paid_at
      t.timestamps
    end
    add_index :payouts, :campaign_id, unique: true

    create_table :api_clients do |t|
      t.references :backer, null: false, foreign_key: true
      t.string :name, null: false
      t.string :key_digest, null: false
      t.datetime :last_used_at
      t.timestamps
    end
    add_index :api_clients, :key_digest, unique: true

    create_table :webhook_events do |t|
      t.string :event_id, null: false
      t.string :kind, null: false
      t.timestamps
    end
    add_index :webhook_events, :event_id, unique: true
  end
end
