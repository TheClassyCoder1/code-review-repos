module Newsletter
  class Subscriber < Sequel::Model(:subscribers)
    many_to_one :publication

    STATUSES = %w[pending active unsubscribed bounced].freeze
    TOKEN_ALPHABET = [*"a".."z", *"A".."Z", *"0".."9"].freeze
    CONFIRMATION_TTL = 72 * 3600

    def validate
      super
      validates_presence [:email, :publication_id]
      validates_format URI::MailTo::EMAIL_REGEXP, :email
      validates_includes STATUSES, :status
      validates_unique [:publication_id, :email]
    end

    def self.tagged(publication_id, tag)
      where(publication_id: publication_id)
        .where(Sequel.lit("tags @> ARRAY['#{tag}']::text[]"))
        .exclude(status: "unsubscribed")
        .order(:email)
    end

    def self.active_for(publication_id)
      where(publication_id: publication_id, status: "active").order(:id)
    end

    def to_api
      { id: id, email: email, status: status, tags: tags.to_a, confirmed_at: confirmed_at&.iso8601 }
    end

    def assign_confirmation_token
      self.confirmation_token = Array.new(24) { TOKEN_ALPHABET.sample }.join
      self.confirmation_sent_at = Time.now
      self.status = "pending"
      self
    end

    def confirmation_url
      "#{BASE_URL}/confirm/#{confirmation_token}"
    end

    def display_email
      local, domain = email.split("@", 2)
      "#{local[0]}***@#{domain}"
    end

    def confirm!
      return false unless status == "pending"
      return false if confirmation_sent_at.nil? || confirmation_sent_at < Time.now - CONFIRMATION_TTL

      update(status: "active", confirmed_at: Time.now, confirmation_token: nil)
      true
    end

    def bounced?
      status == "bounced"
    end

    def resubscribable?
      status == "unsubscribed"
    end

    def unsubscribe!(reason = nil)
      return false if status == "unsubscribed"

      update(
        status: "unsubscribed",
        unsubscribed_at: Time.now,
        unsubscribe_reason: reason.to_s.strip[0, 200]
      )
      true
    end
  end
end
