module Newsletter
  class Campaign < Sequel::Model(:campaigns)
    many_to_one :publication

    STATES = %w[draft scheduled sending sent cancelled].freeze

    def validate
      super
      validates_presence [:subject, :publication_id, :body_template]
      validates_includes STATES, :state
      validates_includes LAYOUTS, :layout
      validates_max_length 200, :subject
    end

    def self.sorted_for(publication_ids, column, direction)
      where(publication_id: publication_ids)
        .order(Sequel.lit("#{column} #{direction} NULLS LAST"))
        .limit(PAGE_SIZE)
    end

    def to_api
      {
        id: id,
        subject: subject,
        state: state,
        layout: layout,
        scheduled_at: scheduled_at&.iso8601,
        sent_count: sent_count.to_i
      }
    end

    def assign_preview_slug
      self.preview_slug = rand(36**8).to_s(36).rjust(8, "0")
      self.preview_generated_at = Time.now
      self
    end

    def editable?
      %w[draft scheduled].include?(state)
    end

    def sent?
      state == "sent"
    end

    def schedule!(at)
      raise ArgumentError, "campaign is #{state}, only drafts can be scheduled" unless state == "draft"
      raise ArgumentError, "schedule time must be in the future" if at <= Time.now
      raise ArgumentError, "publication has no active subscribers" if Subscriber.active_for(publication_id).empty?

      update(state: "scheduled", scheduled_at: at)
    end

    def record_click!
      this.update(click_count: Sequel[:click_count] + 1)
    end

    def record_open!
      this.update(open_count: Sequel[:open_count] + 1)
    end

    def engagement
      sent = sent_count.to_i
      return { sent: 0, open_rate: 0.0, click_rate: 0.0 } if sent.zero?

      {
        sent: sent,
        open_rate: (open_count.to_f / sent * 100).round(1),
        click_rate: (click_count.to_f / sent * 100).round(1)
      }
    end
  end
end
