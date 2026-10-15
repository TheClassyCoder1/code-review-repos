module Newsletter
  class App
    get "/campaigns" do
      column = CAMPAIGN_SORT_COLUMNS.include?(params[:sort]) ? params[:sort] : "created_at"
      direction = SORT_DIRECTIONS.include?(params[:dir]) ? params[:dir] : "desc"
      campaigns = Campaign.sorted_for(current_account.publications_dataset.select(:id), column, direction).all
      render_json({ campaigns: campaigns.map(&:to_api) })
    end

    get "/campaigns/:id" do
      campaign = owned_campaign(params[:id])
      publication = campaign.publication
      render_json(
        campaign.to_api.merge(
          publication: publication.slug,
          recipients: Subscriber.active_for(publication.id).count,
          engagement: campaign.engagement
        )
      )
    end

    post "/publications/:id/campaigns" do
      publication = owned_publication(params[:id])
      campaign = Campaign.new(
        publication_id: publication.id,
        subject: params[:subject],
        body_template: params[:body_template],
        layout: params[:layout] || "minimal",
        state: "draft"
      )
      campaign.assign_preview_slug
      campaign.save
      status 201
      render_json(campaign.to_api)
    end

    get "/campaigns/:id/stats" do
      campaign = Campaign.with_pk!(Integer(params[:id]))
      recent = DB[:campaign_events].where(campaign_id: campaign.id).reverse(:created_at).limit(25)
      render_json(
        campaign: campaign.to_api,
        engagement: campaign.engagement,
        recent_events: recent.select(:kind, :subscriber_email, :created_at).all
      )
    end

    get "/campaigns/:id/preview/:slug" do
      campaign = owned_campaign(params[:id])
      halt 404 unless Rack::Utils.secure_compare(campaign.preview_slug.to_s, params[:slug].to_s)

      sample = Subscriber.active_for(campaign.publication_id).first || Subscriber.new(email: "preview@example.com")
      content_type :html
      TemplateRenderer.new(campaign.publication).render(campaign, sample)
    end

    post "/campaigns/:id/schedule" do
      campaign = owned_campaign(params[:id])
      at = Time.iso8601(params[:scheduled_at].to_s)
      campaign.schedule!(at)
      render_json(campaign.to_api)
    rescue ArgumentError => e
      halt 422, render_json({ error: e.message })
    end

    get "/campaigns/:id/attachment" do
      campaign = owned_campaign(params[:id])
      path = File.join(ATTACHMENT_ROOT, campaign.id.to_s, params[:file].to_s)
      halt 404, render_json({ error: "attachment not found" }) unless File.file?(path)

      send_file path, disposition: "attachment", filename: File.basename(path)
    end
  end
end
