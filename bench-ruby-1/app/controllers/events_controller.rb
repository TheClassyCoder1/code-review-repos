class EventsController < ApplicationController
  SORTABLE_COLUMNS = %w[starts_at title price_cents created_at].freeze
  SORT_DIRECTIONS = %w[asc desc].freeze

  skip_before_action :authenticate_organiser!, only: %i[index show search]

  def index
    column = SORTABLE_COLUMNS.include?(params[:sort]) ? params[:sort] : "starts_at"
    direction = SORT_DIRECTIONS.include?(params[:dir]) ? params[:dir] : "asc"
    events = Event.published.upcoming.sorted(column, direction).limit(100)
    render json: events.as_json(only: %i[id title starts_at venue price_cents])
  end

  def show
    event = Event.published.find(params[:id])
    remaining = event.capacity - event.tickets_sold
    render json: event.as_json(only: %i[id title description_html starts_at ends_at venue price_cents])
                      .merge(remaining: remaining, sold_out: event.sold_out?)
  end

  def search
    term = params[:q].to_s.strip
    return render(json: []) if term.empty?

    render json: Event.search(term).limit(25).as_json(only: %i[id title starts_at venue])
  end

  def preview
    title = params[:title].to_s
    venue = params[:venue].to_s
    starts_at = Time.zone.parse(params[:starts_at].to_s) || Time.zone.now
    markup = "<article class=\"event-preview\"><h1>#{title}</h1>" \
             "<p>#{venue} &middot; #{starts_at.strftime('%d %b %Y %H:%M')}</p></article>"
    render html: markup.html_safe, layout: "preview"
  end
end
