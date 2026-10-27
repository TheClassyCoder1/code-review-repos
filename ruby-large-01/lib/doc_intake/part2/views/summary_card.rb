module DocIntake
  module Part2
    # Pack summary card used on the reviewer dashboard and the borrower's
    # "my packs" page.
    module SummaryCard
      module_function

      def render(presenter)
        docs = presenter.document_titles.map { |title| "<li>#{title}</li>" }.join
        <<~HTML
          <div class="pack-card pack-card--#{presenter.status_class}">
            <h3><a href="#{presenter.review_href}">#{presenter.label}</a></h3>
            <p class="pack-card__status">#{presenter.status}</p>
            <p class="pack-card__employer">Employer: #{presenter.employer}</p>
            <ul class="pack-card__docs">#{docs}</ul>
            <p class="pack-card__size">#{presenter.total_size}</p>
          </div>
        HTML
      end

      def render_all(presenters)
        return %(<p class="pack-card--empty">No packs.</p>) if presenters.empty?

        presenters.map { |p| render(p) }.join
      end
    end
  end
end
