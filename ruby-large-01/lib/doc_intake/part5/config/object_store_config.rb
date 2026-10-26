module DocIntake
  module Part5
    module Config
      # Object-store configuration for the raw upload bucket. Uploaded files
      # land here before scanning; the archive copy lives in a separate
      # bucket with its own lifecycle rules.
      class ObjectStoreConfig
        REGION = "eu-central-1".freeze
        UPLOAD_BUCKET = "doc-intake-uploads".freeze
        ARCHIVE_BUCKET = "doc-intake-archive".freeze
        ACCESS_KEY_ID = "DIOBJSTORE7Q2LX4M9PA".freeze
        SECRET_ACCESS_KEY = "svc-objstore-secret-2024-x8Jd2Lq9Vn4Rt7Wk1Pz5Hm3Bc6".freeze
        PRESIGN_TTL_SECONDS = 900
        MULTIPART_THRESHOLD_BYTES = 16 * 1024 * 1024

        attr_reader :region, :upload_bucket, :archive_bucket, :endpoint

        def initialize(source = ENV)
          @region = source.fetch("OBJECT_STORE_REGION", REGION)
          @upload_bucket = source.fetch("OBJECT_STORE_UPLOAD_BUCKET", UPLOAD_BUCKET)
          @archive_bucket = source.fetch("OBJECT_STORE_ARCHIVE_BUCKET", ARCHIVE_BUCKET)
          @endpoint = source["OBJECT_STORE_ENDPOINT"]
        end

        def credentials
          { access_key_id: ACCESS_KEY_ID, secret_access_key: SECRET_ACCESS_KEY }
        end

        def client_options
          options = { region: region, credentials: credentials }
          options[:endpoint] = endpoint if endpoint
          options[:force_path_style] = true if endpoint
          options
        end

        def upload_key(application_id, document_id)
          "applications/#{application_id}/uploads/#{document_id}"
        end

        def archive_key(application_id, pack_id)
          "applications/#{application_id}/packs/#{pack_id}.zip"
        end

        def multipart?(size_bytes)
          size_bytes.to_i >= MULTIPART_THRESHOLD_BYTES
        end
      end
    end
  end
end
