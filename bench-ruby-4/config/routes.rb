Rails.application.routes.draw do
  resources :plans, only: %i[index show create]

  resources :members, only: %i[create] do
    collection { post :import }
    member { post :upload_photo }

    resources :check_ins, only: %i[index create] do
      collection { post :book }
    end
    resources :memberships, only: %i[show create] do
      member do
        post :cancel
        post :change_plan
        post :pause
      end
    end
    resource :guest_pass, only: %i[show create], controller: "guest_passes"
  end

  scope :kiosk, controller: "kiosk" do
    post :sessions, action: :create_session
    post :scan
  end

  scope "webhooks/gateway", controller: "gateway_webhooks" do
    post :payment_succeeded
    post :payment_failed
  end
end
