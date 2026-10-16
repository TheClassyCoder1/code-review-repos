class ApplicationController < ActionController::API
  rescue_from ActiveRecord::RecordNotFound, with: :not_found
  rescue_from ActiveRecord::RecordNotUnique, with: :conflict
  rescue_from ActiveRecord::RecordInvalid, with: :unprocessable
  rescue_from ActionController::ParameterMissing, with: :bad_request

  private

  def current_member
    @current_member ||= Member.find(params[:member_id])
  end

  def not_found
    head :not_found
  end

  def conflict
    render json: { error: "conflict" }, status: :conflict
  end

  def unprocessable(error)
    render json: { errors: error.record.errors.full_messages }, status: :unprocessable_entity
  end

  def bad_request(error)
    render json: { error: error.message }, status: :bad_request
  end
end
