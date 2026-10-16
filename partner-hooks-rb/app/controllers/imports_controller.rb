class ImportsController < ApplicationController
  def create
    importer = PayloadImporter.new(current_partner)
    result = importer.import(request.raw_post, format: params[:format_version])
    render json: result.to_h, status: :created
  rescue ArgumentError, JSON::ParserError => e
    render json: { error: e.message }, status: :unprocessable_entity
  end
end
