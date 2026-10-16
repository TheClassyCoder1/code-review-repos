module Newsletter
  class App
    get "/publications/:id/subscribers" do
      publication = owned_publication(params[:id])
      page = [params.fetch(:page, 1).to_i, 1].max
      dataset = publication.subscribers_dataset.order(Sequel.desc(:created_at))
      dataset = dataset.where(status: params[:status]) if Subscriber::STATUSES.include?(params[:status])
      paged = dataset.paginate(page, PAGE_SIZE)
      render_json({ page: page, total: paged.pagination_record_count, subscribers: paged.map(&:to_api) })
    end

    get "/publications/:id/subscribers/tagged" do
      publication = owned_publication(params[:id])
      subscribers = Subscriber.tagged(publication.id, params[:tag].to_s).limit(PAGE_SIZE).all
      render_json({ tag: params[:tag], subscribers: subscribers.map(&:to_api) })
    end

    post "/subscribe/:slug" do
      publication = Publication.first!(slug: params[:slug].to_s)
      email = params[:email].to_s.strip.downcase
      existing = publication.subscribers_dataset.first(email: email)
      halt 200, render_json({ status: existing.status }) if existing && !existing.resubscribable?

      subscriber = existing || Subscriber.new(publication_id: publication.id, email: email)
      subscriber.first_name = params[:first_name].to_s.strip[0, 80]
      subscriber.assign_confirmation_token
      subscriber.save
      DB[:outbound_emails].insert(kind: "confirmation", subscriber_id: subscriber.id, created_at: Time.now)
      status 202
      render_json({ status: subscriber.status })
    end

    get "/confirm/:token" do
      subscriber = Subscriber.first(confirmation_token: params[:token].to_s)
      halt 404, render_json({ error: "unknown confirmation link" }) unless subscriber

      if subscriber.confirm!
        render_json({ status: "active" })
      else
        halt 410, render_json({ error: "confirmation link expired" })
      end
    end
  end
end
