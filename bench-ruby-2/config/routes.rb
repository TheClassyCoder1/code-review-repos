Rails.application.routes.draw do
  resources :campaigns, only: %i[index show create] do
    member do
      get :rewards
      get :share_card
      post :publish
    end

    resources :exports, only: [] do
      collection do
        get :backers
        get :rewards_report
      end
    end
  end

  resources :pledges, only: %i[index create destroy] do
    patch :change_reward, on: :member
  end

  resources :payouts, only: %i[index create]
  resources :api_clients, only: %i[create destroy]

  post "/webhooks/gateway", to: "webhooks#create"
end
