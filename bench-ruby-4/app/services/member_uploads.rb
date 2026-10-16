require "csv"
require "digest"

class MemberUploads
  ROSTER_HEADERS = %w[first_name last_name email phone plan_code].freeze
  PHOTO_TYPES = { "image/jpeg" => ".jpg", "image/png" => ".png" }.freeze
  PHOTO_DIR = Rails.root.join("storage", "member_photos")

  def import_roster(upload)
    rows = CSV.parse(upload.read, headers: true)
    missing = ROSTER_HEADERS - rows.headers.compact.map(&:strip)
    raise ArgumentError, "missing columns: #{missing.join(", ")}" if missing.any?

    plans = Plan.available.index_by(&:code)
    rows.each_with_object({ created: 0, skipped: 0 }) do |row, totals|
      plan = plans[row["plan_code"].to_s.strip]
      member = Member.find_or_initialize_by(email: row["email"].to_s.strip.downcase)
      if plan.nil? || member.persisted?
        totals[:skipped] += 1
        next
      end

      Member.transaction do
        member.update!(first_name: row["first_name"], last_name: row["last_name"], phone: row["phone"])
        member.memberships.create!(plan: plan, status: "active", starts_on: Date.current, billing_day: [Date.current.day, 28].min)
      end
      totals[:created] += 1
    end
  end

  def roster_template
    CSV.generate do |csv|
      csv << ROSTER_HEADERS
      csv << ["Jane", "Doe", "jane@example.com", "+15555550100", "basic-monthly"]
    end
  end

  def store_photo(member, upload)
    data = upload.read
    content_type = Marcel::MimeType.for(StringIO.new(data), name: upload.original_filename)
    extension = PHOTO_TYPES[content_type]
    raise ArgumentError, "unsupported photo type" unless extension

    key = "#{member.id}-#{Digest::SHA256.hexdigest(data)[0, 16]}#{extension}"
    FileUtils.mkdir_p(PHOTO_DIR)
    File.binwrite(PHOTO_DIR.join(key), data)
    member.update!(photo_key: key)
    key
  end
end
