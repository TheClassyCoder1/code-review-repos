module Newsletter
  class App
    get "/admin/accounts" do
      page = [params.fetch(:page, 1).to_i, 1].max
      accounts = Account.order(Sequel.desc(:created_at)).paginate(page, PAGE_SIZE)
      render_json({
        page: page,
        total: accounts.pagination_record_count,
        accounts: accounts.map(&:to_api)
      })
    end

    post "/admin/accounts/:id/disable" do
      account = Account.with_pk!(params[:id].to_i)
      account.disable!(params[:reason])
      render_json(account.to_api)
    end

    delete "/admin/subscribers/:id" do
      subscriber = Subscriber.with_pk!(params[:id].to_i)
      DB.transaction do
        DB[:campaign_events].where(subscriber_email: subscriber.email).delete
        subscriber.destroy
      end
      Newsletter.logger.info("admin erased subscriber id=#{subscriber.id} by account=#{current_account.id}")
      status 204
    end
  end
end
