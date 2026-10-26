require "digest"
require_relative "../../../lib/doc_intake/part5/archive/manifest_builder"
require_relative "../fixtures/archive_profiles"

RSpec.describe DocIntake::Part5::Archive::ManifestBuilder do
  let(:fixed_time) { Time.utc(2026, 10, 1, 12, 0, 0) }
  let(:builder) { described_class.new(clock: -> { fixed_time }) }
  let(:pack) { DocIntake::Part5::SpecFixtures::ArchiveProfiles.sample_pack }

  describe "#build_manifest" do
    it "is deterministic for the same pack" do
      expect(builder.build_manifest(pack)).to eq(builder.build_manifest(pack))
    end

    it "lists files sorted by archive path" do
      manifest = JSON.parse(builder.build_manifest(pack))
      paths = manifest["files"].map { |file| file["path"] }
      expect(paths).to eq(paths.sort)
      expect(manifest["file_count"]).to eq(3)
      expect(manifest["total_bytes"]).to eq(548_000)
    end

    it "rejects duplicate document ids" do
      duplicated = pack.merge(documents: pack[:documents] + [pack[:documents].first])
      expect { builder.build_manifest(duplicated) }
        .to raise_error(described_class::ManifestError, /duplicate/)
    end

    it "rejects kinds that would escape the archive folder" do
      bad = pack.merge(documents: [pack[:documents].first.merge(kind: "../etc")])
      expect { builder.build_manifest(bad) }
        .to raise_error(described_class::ManifestError, /unsafe/)
    end
  end

  describe "#verify" do
    let(:bytes) { "%PDF-1.7 sample" }
    let(:single_pack) do
      pack.merge(documents: [
        { id: "doc-9", kind: "payslip", size_bytes: bytes.bytesize, sha256: Digest::SHA256.hexdigest(bytes) }
      ])
    end
    let(:manifest) { builder.build_manifest(single_pack) }

    it "accepts an intact archive" do
      result = builder.verify(manifest, "payslip/doc-9.pdf" => bytes)
      expect(result[:ok]).to be(true)
    end

    it "reports corrupted, missing and unexpected files separately" do
      corrupted = builder.verify(manifest, "payslip/doc-9.pdf" => "#{bytes}x")
      expect(corrupted[:corrupted]).to eq(["payslip/doc-9.pdf"])

      missing = builder.verify(manifest, {})
      expect(missing[:missing]).to eq(["payslip/doc-9.pdf"])

      extra = builder.verify(manifest, "payslip/doc-9.pdf" => bytes, "id_scan/x.pdf" => "x")
      expect(extra[:unexpected]).to eq(["id_scan/x.pdf"])
      expect(extra[:ok]).to be(false)
    end
  end
end
