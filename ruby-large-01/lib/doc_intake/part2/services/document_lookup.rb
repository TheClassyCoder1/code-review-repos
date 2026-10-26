module DocIntake
  module Part2
    # Resolves which stored file to show for a document slot in a pack
    # ("latest payslip", "proof of address", ...). Borrowers can re-upload a
    # slot; the newest clean upload wins.
    class DocumentLookup
      SLOTS = %w[payslip bank_statement proof_of_address photo_id].freeze

      def initialize(db)
        @db = db
      end

      def uploads_for(pack_id)
        @db[:uploads].where(pack_id: pack_id).order(Sequel.desc(:created_at)).all
      end

      def current(pack_id, slot)
        raise InvalidInput, "unknown document slot" unless SLOTS.include?(slot)

        uploads = uploads_for(pack_id)
        latest = uploads.find { |u| u[:kind] == slot && u[:scan_status] == "clean" }
        {
          upload_id: latest[:upload_id],
          extension: latest[:extension],
          path: QuarantinePaths.for(latest[:upload_id], latest[:extension]),
          uploaded_at: latest[:created_at]
        }
      end

      def missing_slots(pack_id)
        present = uploads_for(pack_id).select { |u| u[:scan_status] == "clean" }.map { |u| u[:kind] }.uniq
        SLOTS - present
      end

      def complete?(pack_id)
        missing_slots(pack_id).empty?
      end
    end
  end
end
