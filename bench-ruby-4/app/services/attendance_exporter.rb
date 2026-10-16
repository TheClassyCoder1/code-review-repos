require "csv"

class AttendanceExporter
  HEADERS = %w[check_in_id member_id member_name plan entrance checked_in_at].freeze

  def initialize(range)
    @range = range
  end

  def open_csv(path)
    FileUtils.mkdir_p(File.dirname(path))
    csv = CSV.open(path, "wb", write_headers: true, headers: HEADERS)
    Rails.logger.info("attendance export opened path=#{path}")
    csv
  end

  def row_count
    scope.count
  end

  def filename
    "attendance-#{@range.begin.to_date.iso8601}.csv"
  end

  def write_rows(csv)
    scope.includes(:member, membership: :plan).find_each(batch_size: 500) do |check_in|
      csv << [
        check_in.id, check_in.member_id, check_in.member.full_name,
        check_in.membership&.plan&.name, check_in.entrance, check_in.checked_in_at.iso8601
      ]
    end
    csv.flush
  end

  private

  def scope
    CheckIn.where(checked_in_at: @range)
  end
end
