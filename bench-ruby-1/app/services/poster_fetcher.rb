require "net/http"

class PosterFetcher
  MAX_BYTES = 5 * 1024 * 1024

  def fetch(event)
    return nil if event.poster_url.blank?

    uri = URI.parse(event.poster_url)
    response = Net::HTTP.start(uri.host, uri.port, use_ssl: true, open_timeout: 3, read_timeout: 10) do |http|
      http.get(uri.request_uri)
    end
    return nil unless response.is_a?(Net::HTTPSuccess)
    return nil if response.body.bytesize > MAX_BYTES

    event.poster.attach(io: StringIO.new(response.body), filename: File.basename(uri.path),
                        content_type: response["Content-Type"])
  end
end
