module DocIntake
  module Part5
    module SpecFixtures
      # Environment hashes used by the archive and export specs. They are
      # passed explicitly to the settings objects instead of mutating ENV, so
      # specs can run in parallel.
      module ArchiveProfiles
        module_function

        def test_env(overrides = {})
          {
            "DOC_INTAKE_PROFILE" => "test",
            "ARCHIVE_DATABASE_URL" => "sqlite://tmp/archive_test.db",
            "ARCHIVE_DB_PASSWORD" => "changeme",
            "OBJECT_STORE_ENDPOINT" => "http://127.0.0.1:9000",
            "OBJECT_STORE_UPLOAD_BUCKET" => "spec-uploads",
            "OBJECT_STORE_ARCHIVE_BUCKET" => "spec-archive"
          }.merge(overrides)
        end

        def staging_env_with_placeholder
          test_env("DOC_INTAKE_PROFILE" => "staging")
        end

        def sample_pack(id: "pack-0001", application_id: "app-1001")
          {
            id: id,
            application_id: application_id,
            product: "personal",
            employment_type: "salaried",
            documents: [
              { id: "doc-1", kind: "id_scan", size_bytes: 120_000, sha256: "a" * 64 },
              { id: "doc-2", kind: "bank_statement", size_bytes: 340_000, sha256: "b" * 64 },
              { id: "doc-3", kind: "payslip", size_bytes: 88_000, sha256: "c" * 64 }
            ]
          }
        end
      end
    end
  end
end
