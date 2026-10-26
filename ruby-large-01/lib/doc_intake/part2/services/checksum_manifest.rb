require "digest"

module DocIntake
  module Part2
    # Produces a sha256sum-compatible manifest for the files in a pack
    # archive, and verifies an extracted archive against one.
    class ChecksumManifest
      BUFFER = 1024 * 1024

      def build(paths)
        paths.map { |path| "#{checksum(path)}  #{File.basename(path)}" }.join("\n") + "\n"
      end

      def verify(manifest_text, directory)
        failures = []
        manifest_text.each_line do |line|
          expected, name = line.strip.split(/\s+/, 2)
          next if expected.nil? || name.nil?

          path = File.join(directory, File.basename(name))
          failures << name unless File.exist?(path) && checksum(path) == expected
        end
        raise IntegrityError, "#{failures.size} archive entries failed verification" unless failures.empty?

        true
      end

      private

      def checksum(path)
        digest = Digest::SHA256.new
        io = File.open(path, "rb")
        while (chunk = io.read(BUFFER))
          digest.update(chunk)
        end
        digest.hexdigest
      end
    end
  end
end
