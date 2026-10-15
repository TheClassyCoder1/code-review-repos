Rails.application.routes.draw do
  resources :events, only: %i[index show] do
    collection do
      get :search
      post :preview
    end
  end

  resources :tickets, only: %i[index create] do
    member do
      get :download
    end
    collection do
      post :accept_transfer
    end
  end

  resources :refunds, only: %i[index show create]
  resources :reports, only: %i[index show]
  resources :imports, only: %i[create]
  resources :api_keys, only: %i[create]

  resource :organiser, only: %i[show update] do
    post :test_webhook
  end

  resource :session, only: %i[show create destroy]

  namespace :admin do
    resources :events, only: %i[index show destroy]
  end
end
