require "csv"
require "fileutils"
require "open3"

module Newsletter
  class ExportArchiver
    COLUMNS = %w[email status tags confirmed_at unsubscribed_at].freeze

    def initialize(export_dir: EXPORT_DIR)
      @export_dir = export_dir
      FileUtils.mkdir_p(@export_dir)
    end

    def write_csv(publication)
      path = File.join(@export_dir, "#{publication.slug}.csv")
      CSV.open(path, "w") do |csv|
        csv << COLUMNS
        publication.subscribers_dataset.order(:id).paged_each(rows_per_fetch: 1_000) do |subscriber|
          csv << [subscriber.email, subscriber.status, subscriber.tags.to_a.join("|"),
                  subscriber.confirmed_at&.iso8601, subscriber.unsubscribed_at&.iso8601]
        end
      end
      path
    end

    def export_path(publication)
      path = File.join(@export_dir, "#{publication.slug}.csv.gz")
      File.exist?(path) ? path : nil
    end

    def compress_csv(publication)
      `gzip -kf #{@export_dir}/#{publication.slug}.csv`
      raise "gzip failed for publication #{publication.id}" unless $?.success?

      File.join(@export_dir, "#{publication.slug}.csv.gz")
    end

    def archive_name(publication)
      stamp = Time.now.utc.strftime("%Y%m%d%H%M%S")
      "#{publication.slug}-#{stamp}"
    end

    def archive(publication, label)
      csv = write_csv(publication)
      target = File.join(@export_dir, "#{label}.tar.gz")
      output, status = Open3.capture2e("tar -czf #{target} -C #{@export_dir} #{File.basename(csv)}")
      raise "archive failed: #{output.lines.last}" unless status.success?

      target
    end
  end
end
