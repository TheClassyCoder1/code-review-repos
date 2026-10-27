require "net/http"
require "rss"
require "cgi"

module Newsletter
  class FeedImporter
    MAX_ITEMS = 20

    Item = Struct.new(:title, :link, :summary, :published_at, keyword_init: true)

    def preview(url)
      uri = URI.parse(url.to_s.strip)
      response = Net::HTTP.get_response(uri)
      raise ArgumentError, "feed returned HTTP #{response.code}" unless response.is_a?(Net::HTTPSuccess)

      { url: uri.to_s, content_type: response["content-type"], items: parse_items(response.body).first(5) }
    end

    def http_for(uri)
      http = Net::HTTP.new(uri.host, uri.port)
      http.use_ssl = uri.scheme == "https"
      http.open_timeout = 5
      http.read_timeout = 10
      http
    end

    def refresh(publication)
      uri = URI.parse(publication.feed_url)
      response = http_for(uri).request(Net::HTTP::Get.new(uri.request_uri))
      return [] unless response.is_a?(Net::HTTPSuccess)

      items = parse_items(response.body)
      publication.update(feed_checked_at: Time.now, feed_item_count: items.size)
      items
    end

    def to_digest(items)
      items.map { |item| "<li><a href=\"#{CGI.escapeHTML(item.link)}\">#{CGI.escapeHTML(item.title)}</a></li>" }
           .join("\n")
    end

    def parse_items(xml)
      feed = RSS::Parser.parse(xml, false)
      return [] unless feed

      feed.items.first(MAX_ITEMS).map do |entry|
        Item.new(
          title: entry.respond_to?(:title) && entry.title.respond_to?(:content) ? entry.title.content : entry.title.to_s,
          link: entry.link.respond_to?(:href) ? entry.link.href : entry.link.to_s,
          summary: entry.respond_to?(:description) ? entry.description.to_s[0, 500] : "",
          published_at: entry.respond_to?(:pubDate) ? entry.pubDate : nil
        )
      end
    rescue RSS::Error
      []
    end
  end
end
