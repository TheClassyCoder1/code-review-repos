class WebhookEvent < ApplicationRecord
  validates :event_id, :kind, presence: true
end
