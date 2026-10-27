require_relative "../../../lib/doc_intake/part5/config/placeholder_guard"
require_relative "../../../lib/doc_intake/part5/config/export_signing"
require_relative "../fixtures/archive_profiles"

RSpec.describe DocIntake::Part5::Config::PlaceholderGuard do
  let(:fixtures) { DocIntake::Part5::SpecFixtures::ArchiveProfiles }

  def guard_for(env)
    described_class.new(profile: DocIntake::Part5::Config::Profile.new(env))
  end

  it "accepts placeholder credentials under the test profile" do
    env = fixtures.test_env
    expect(guard_for(env).check!(env)).to be(true)
  end

  it "rejects placeholder credentials in a deployed profile" do
    env = fixtures.staging_env_with_placeholder
    expect { guard_for(env).check!(env) }
      .to raise_error(described_class::PlaceholderError, /ARCHIVE_DB_PASSWORD/)
  end

  it "does not echo the offending value" do
    env = fixtures.staging_env_with_placeholder
    expect { guard_for(env).check!(env) }
      .to raise_error(described_class::PlaceholderError) { |error| expect(error.message).not_to include("changeme") }
  end

  it "treats a missing profile as production" do
    profile = DocIntake::Part5::Config::Profile.new({})
    expect(profile.name).to eq("production")
    expect(profile).to be_deployed
  end

  describe "export signing key" do
    it "falls back to the dummy key only in the test profile" do
      signing = DocIntake::Part5::Config::ExportSigning.new(fixtures.test_env)
      expect(signing.sign("payload")).to match(/\A[0-9a-f]{64}\z/)
    end

    it "refuses to start without a key in staging" do
      env = { "DOC_INTAKE_PROFILE" => "staging" }
      expect { DocIntake::Part5::Config::ExportSigning.new(env) }
        .to raise_error(DocIntake::Part5::Config::Profile::ProfileError, /EXPORT_SIGNING_KEY/)
    end

    it "uses the configured key when present" do
      env = { "DOC_INTAKE_PROFILE" => "staging", "EXPORT_SIGNING_KEY" => "k" * 32 }
      signing = DocIntake::Part5::Config::ExportSigning.new(env)
      signature = signing.sign("payload")
      expect(signing.valid?("payload", signature)).to be(true)
      expect(signing.valid?("tampered", signature)).to be(false)
    end
  end
end
