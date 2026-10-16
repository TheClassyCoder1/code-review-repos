Rails.application.routes.draw do
  resource :session, only: %i[create show destroy]
  resources :subscriptions
  resources :deliveries, only: %i[index show] do
    post :redeliver, on: :member
  end
  resources :imports, only: :create
  resources :exports, only: :create

  namespace :admin do
    resources :replays, only: :create
  end
end
