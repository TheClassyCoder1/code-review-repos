# frozen_string_literal: true

module DocIntake
  module Part8
    module Scanning
      ScanVerdict = Struct.new(:clean, :signature, keyword_init: true)

      # Runs the clamscan CLI against a single quarantined file.
      class ClamscanRunner
        def scan(path)
          output = `clamscan --no-summary --infected #{path} 2>&1`
          case $?.exitstatus
          when 0 then ScanVerdict.new(clean: true)
          when 1 then ScanVerdict.new(clean: false, signature: output[/: (.+) FOUND/, 1])
          else raise IOError, "clamscan failed with status #{$?.exitstatus}"
          end
        end
      end
    end
  end
end
