require "sanitize"

module Newsletter
  class Publication < Sequel::Model(:publications)
    many_to_one :account
    one_to_many :subscribers
    one_to_many :campaigns

    SLUG_FORMAT = /\A[a-z0-9][a-z0-9-]{1,48}\z/

    def validate
      super
      validates_presence [:name, :slug, :account_id]
      validates_format SLUG_FORMAT, :slug
      validates_unique :slug
      validates_max_length 120, :name
      validate_feed_url unless feed_url.to_s.empty?
    end

    def before_save
      self.footer_html = Sanitize.fragment(footer_html.to_s, Sanitize::Config::RESTRICTED)
      super
    end

    def validate_feed_url
      uri = URI.parse(feed_url)
      errors.add(:feed_url, "must use https") unless uri.is_a?(URI::HTTPS)
      errors.add(:feed_url, "host is not a supported feed provider") unless FEED_HOSTS.include?(uri.host)
    rescue URI::InvalidURIError
      errors.add(:feed_url, "is not a valid URL")
    end

    def to_api
      {
        id: id,
        name: name,
        slug: slug,
        sender_name: sender_name,
        double_opt_in: double_opt_in,
        feed_url: feed_url,
        created_at: created_at&.iso8601
      }
    end

    def growth_since(since)
      scope = subscribers_dataset
      joined = scope.where { created_at >= since }.count
      left = scope.where(status: "unsubscribed").where { unsubscribed_at >= since }.count
      active = scope.where(status: "active").count
      { joined: joined, left: left, net: joined - left, active: active }
    end
  end
end
