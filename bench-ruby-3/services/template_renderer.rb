require "erb"
require "cgi"

module Newsletter
  class TemplateRenderer
    def initialize(publication)
      @publication = publication
    end

    def layout_source(name)
      path = File.join(TEMPLATE_DIR, "layouts", "#{name}.html.erb")
      File.read(path)
    end

    def render(campaign, subscriber)
      body = render_body(campaign, subscriber)
      layout = ERB.new(layout_source(campaign.layout))
      layout.result_with_hash(
        subject: CGI.escapeHTML(campaign.subject),
        body: body,
        footer: footer_html,
        unsubscribe_url: CampaignMailer.unsubscribe_url(subscriber)
      )
    end

    def render_body(campaign, subscriber)
      template = ERB.new(campaign.body_template, trim_mode: "-")
      template.result_with_hash(
        first_name: subscriber.first_name.to_s,
        email: subscriber.email,
        publication_name: @publication.name
      )
    end

    def preheader(campaign)
      text = plain_text(campaign.body_template)
      text.length > 90 ? "#{text[0, 87]}..." : text
    end

    def footer_html
      address = CGI.escapeHTML(@publication.postal_address.to_s)
      "<footer class=\"nl-footer\">#{@publication.footer_html}<p>#{address}</p></footer>"
    end

    def subject_line(campaign)
      CGI.escapeHTML(campaign.subject.to_s.strip)
    end

    def link_rewriter(campaign)
      ->(url) { "#{BASE_URL}/c/#{campaign.id}?url=#{CGI.escape(url)}" }
    end

    def plain_text(html)
      html.to_s
          .gsub(%r{<(script|style)[^>]*>.*?</\1>}mi, "")
          .gsub(/<br\s*\/?>|<\/p>/i, "\n")
          .gsub(/<[^>]+>/, "")
          .gsub(/&nbsp;/, " ")
          .squeeze(" ")
          .strip
    end
  end
end
