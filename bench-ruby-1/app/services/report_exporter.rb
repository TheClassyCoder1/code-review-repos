require "csv"

class ReportExporter
  def self.generated_at(organiser, type)
    path = File.join(Ticketing::REPORT_DIR, organiser.id.to_s, "#{type}.csv")
    File.exist?(path) ? File.mtime(path) : nil
  end

  def initialize(organiser)
    @organiser = organiser
  end

  def read_report(type)
    path = File.join(Ticketing::REPORT_DIR, @organiser.id.to_s, "#{type}.csv")
    return nil unless File.file?(path)

    File.read(path)
  end

  def write_sales_report
    rows = @organiser.tickets.includes(:event).where(status: %w[valid checked_in])
    dir = File.join(Ticketing::REPORT_DIR, @organiser.id.to_s)
    FileUtils.mkdir_p(dir)
    CSV.open(File.join(dir, "sales.csv"), "w") do |csv|
      csv << %w[event reference holder tier price_cents]
      rows.find_each do |t|
        csv << [t.event.title, t.reference, t.holder_name, t.tier, t.event.price_cents]
      end
    end
  end
end
