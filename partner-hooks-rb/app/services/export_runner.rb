require "csv"
require "fileutils"

class ExportRunner
  COLUMNS = %w[id event_type status attempts response_code created_at delivered_at].freeze

  def initialize(partner)
    @partner = partner
    @dir = File.join(PartnerHooks::EXPORT_DIR, partner.id.to_s)
  end

  def archive_deliveries(since)
    write_csv(since)
    archive = File.join(@dir, "#{@partner.slug}-deliveries.tgz")
    system("tar -czf #{archive} -C #{@dir} deliveries.csv", exception: true)
    archive
  end

  def purge_older_than(age)
    Dir.glob(File.join(@dir, "*.tgz")).each do |path|
      File.delete(path) if File.mtime(path) < age.ago
    end
  end

  def archive_with_label(label, since)
    write_csv(since)
    archive = File.join(@dir, "#{label}.tgz")
    system("tar -czf #{archive} -C #{@dir} deliveries.csv", exception: true)
    archive
  end

  private

  def write_csv(since)
    FileUtils.mkdir_p(@dir)
    CSV.open(File.join(@dir, "deliveries.csv"), "w") do |csv|
      csv << COLUMNS
      @partner.deliveries.where("deliveries.created_at >= ?", since).find_each do |delivery|
        csv << COLUMNS.map { |column| delivery.public_send(column) }
      end
    end
  end
end
