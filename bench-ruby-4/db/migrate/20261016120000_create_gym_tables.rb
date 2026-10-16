class CreateGymTables < ActiveRecord::Migration[7.2]
  def change
    create_table :plans do |t|
      t.string :name, null: false
      t.string :code, null: false, index: { unique: true }
      t.integer :price_cents, null: false
      t.integer :cycle_days, null: false, default: 30
      t.integer :guest_passes_per_cycle, null: false, default: 0
      t.boolean :off_peak_only, null: false, default: false
      t.boolean :visit_pack, null: false, default: false
      t.boolean :active, null: false, default: true
      t.timestamps
    end

    create_table :members do |t|
      t.string :first_name, null: false
      t.string :last_name, null: false
      t.string :email, null: false, index: { unique: true }
      t.string :phone
      t.string :badge_id, index: { unique: true }
      t.string :time_zone
      t.string :photo_key
      t.integer :visits_remaining
      t.datetime :last_visit_at
      t.timestamps
    end

    create_table :memberships do |t|
      t.references :member, null: false, foreign_key: true
      t.references :plan, null: false, foreign_key: true
      t.string :status, null: false
      t.date :starts_on, null: false
      t.date :ends_on
      t.integer :billing_day, null: false
      t.string :gateway_subscription_id
      t.datetime :cancelled_at
      t.string :cancel_reason
      t.timestamps
    end
    add_index :memberships, :member_id, unique: true, where: "status = 'active'", name: "index_memberships_one_active_per_member"

    create_table :billing_cycles do |t|
      t.references :membership, null: false, foreign_key: true
      t.date :starts_on, null: false
      t.date :ends_on, null: false
      t.date :due_on, null: false
      t.decimal :amount_due, precision: 10, scale: 2, null: false
      t.decimal :credit_applied, precision: 10, scale: 2, null: false, default: 0
      t.string :status, null: false, default: "pending"
      t.text :notes
      t.integer :lock_version, null: false, default: 0
      t.timestamps
    end

    create_table :payments do |t|
      t.references :billing_cycle, null: false, foreign_key: true
      t.string :gateway_charge_id, null: false
      t.decimal :amount, precision: 10, scale: 2, null: false
      t.datetime :paid_at, null: false
      t.timestamps
    end
    add_index :payments, :gateway_charge_id, unique: true

    create_table :check_ins do |t|
      t.references :member, null: false, foreign_key: true
      t.references :membership, foreign_key: true
      t.string :entrance, null: false
      t.datetime :checked_in_at, null: false
      t.boolean :guest, null: false, default: false
      t.string :guest_name
      t.datetime :gate_opened_at
      t.timestamps
    end
    add_index :check_ins, %i[member_id checked_in_at]

    create_table :fitness_classes do |t|
      t.string :name, null: false
      t.datetime :starts_at, null: false
      t.integer :capacity, null: false
      t.timestamps
    end

    create_table :class_bookings do |t|
      t.references :fitness_class, null: false, foreign_key: true
      t.references :member, null: false, foreign_key: true
      t.string :status, null: false, default: "booked"
      t.timestamps
    end

    create_table :membership_freezes do |t|
      t.references :membership, null: false, foreign_key: true
      t.date :starts_on, null: false
      t.date :ends_on, null: false
      t.string :reason
      t.timestamps
    end

    create_table :kiosk_devices do |t|
      t.string :serial, null: false, index: { unique: true }
      t.string :api_secret, null: false
      t.string :location, null: false
      t.boolean :active, null: false, default: true
      t.timestamps
    end

    create_table :kiosk_sessions do |t|
      t.references :kiosk_device, null: false, foreign_key: true
      t.string :token_digest, null: false
      t.datetime :expires_at, null: false
      t.timestamps
    end
  end
end
