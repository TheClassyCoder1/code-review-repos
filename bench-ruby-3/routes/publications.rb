require "yaml"

module Newsletter
  class App
    get "/publications" do
      publications = current_account.publications_dataset.order(:name).all
      render_json({ publications: publications.map(&:to_api) })
    end

    post "/publications" do
      defaults = PublicationDefaults.load(DEFAULTS_FILE)
      publication = Publication.create(
        account_id: current_account.id,
        name: params[:name],
        slug: params[:slug].to_s.downcase,
        sender_name: params[:sender_name] || defaults["sender_name"],
        double_opt_in: defaults.fetch("double_opt_in", true),
        footer_html: params[:footer_html] || defaults["footer_html"],
        feed_url: params[:feed_url]
      )
      status 201
      render_json(publication.to_api)
    end

    post "/publications/:id/settings/import" do
      publication = owned_publication(params[:id])
      settings = YAML.unsafe_load(request.body.read)
      halt 422, render_json({ error: "settings must be a mapping" }) unless settings.is_a?(Hash)

      allowed = settings.slice("sender_name", "reply_to", "footer_html", "postal_address")
      publication.update(allowed)
      render_json(publication.to_api)
    end

    get "/publications/:id/growth" do
      publication = owned_publication(params[:id])
      days = params.fetch(:days, 30).to_i.clamp(1, 365)
      render_json(publication.growth_since(Time.now - days * 86_400))
    end

    post "/publications/:id/feed/preview" do
      owned_publication(params[:id])
      render_json(FeedImporter.new.preview(params[:url]))
    end

    post "/publications/:id/feed/refresh" do
      publication = owned_publication(params[:id])
      halt 422, render_json({ error: "publication has no feed" }) if publication.feed_url.to_s.empty?

      items = FeedImporter.new.refresh(publication)
      render_json({ imported: items.size })
    end

    post "/publications/:id/exports" do
      publication = owned_publication(params[:id])
      archiver = ExportArchiver.new
      if params[:format] == "gzip"
        archiver.write_csv(publication)
        path = archiver.compress_csv(publication)
      else
        path = archiver.archive(publication, params[:label] || archiver.archive_name(publication))
      end
      send_file path, disposition: "attachment"
    end
  end
end
