module EventsHelper
  def event_date_range(event)
    starts = event.starts_at.in_time_zone(event.time_zone || "Europe/London")
    ends = event.ends_at&.in_time_zone(event.time_zone || "Europe/London")
    return starts.strftime("%a %-d %b %Y, %H:%M") if ends.nil?
    return "#{starts.strftime('%a %-d %b %Y, %H:%M')} - #{ends.strftime('%H:%M')}" if starts.to_date == ends.to_date

    "#{starts.strftime('%-d %b %Y')} - #{ends.strftime('%-d %b %Y')}"
  end

  def price_label(event)
    return "Free" if event.price_cents.zero?

    number_to_currency(event.price_cents / 100.0, unit: "£")
  end

  def formatted_description(event)
    return "" if event.description_html.blank?

    content_tag(:div, event.description_html.html_safe, class: "event-description")
  end
end
