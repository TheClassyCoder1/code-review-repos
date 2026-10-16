class MembersController < ApplicationController
  before_action :check_photo_size, only: :upload_photo

  def create
    member = Member.create!(params.require(:member).permit(:first_name, :last_name, :email, :phone, :time_zone))
    render json: { id: member.id }, status: :created
  end

  def import
    totals = MemberUploads.new.import_roster(params.require(:file))
    render json: totals
  end

  def upload_photo
    key = MemberUploads.new.store_photo(current_member, params.require(:photo))
    render json: { photo_key: key }
  end

  private

  def current_member
    @current_member ||= Member.find(params[:id])
  end

  def check_photo_size
    photo = params[:photo]
    return head(:bad_request) unless photo.respond_to?(:size)

    head :payload_too_large if photo.size > Gym::PHOTO_MAX_BYTES
  end
end
