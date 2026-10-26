require "rspec"
require_relative "../../lib/doc_intake/part7/resubmission_request"
require_relative "../../lib/doc_intake/part7/upload_link_signer"

RSpec.describe DocIntake::Part7::UploadLinkSigner do
  let(:now) { Time.utc(2026, 10, 1, 12, 0, 0) }
  let(:clock_time) { now }
  let(:signer) do
    described_class.new(
      secret: "intake-link-secret-for-tests-0123456789",
      base_url: "https://portal.intake.test/",
      ttl: 3600,
      clock: -> { clock_time }
    )
  end
  let(:request) do
    DocIntake::Part7::ResubmissionRequest.new(
      id: 41,
      application_id: 9001,
      pack_id: 77,
      document_kind: "payslip"
    )
  end

  it "rejects short secrets" do
    expect do
      described_class.new(secret: "short", base_url: "https://x.test")
    end.to raise_error(ArgumentError)
  end

  it "builds a portal link with the token" do
    expect(signer.link_for(request)).to start_with("https://portal.intake.test/portal/part7/resubmit?token=")
  end

  it "round-trips the claims" do
    claims = signer.verify(signer.token_for(request))
    expect(claims).to include("rid" => 41, "app" => 9001, "kind" => "payslip")
  end

  it "rejects a tampered body" do
    body, sig = signer.token_for(request).split(".")
    forged = Base64.urlsafe_encode64(JSON.generate(rid: 42, app: 9001, kind: "payslip", exp: now.to_i + 60), padding: false)
    expect(body).not_to eq(forged)
    expect { signer.verify("#{forged}.#{sig}") }.to raise_error(DocIntake::Part7::InvalidUploadLink)
  end

  it "rejects malformed tokens" do
    expect { signer.verify("not-a-token") }.to raise_error(DocIntake::Part7::InvalidUploadLink)
    expect { signer.verify(nil) }.to raise_error(DocIntake::Part7::InvalidUploadLink)
  end

  context "after the ttl" do
    it "rejects the link" do
      token = signer.token_for(request)
      later = described_class.new(
        secret: "intake-link-secret-for-tests-0123456789",
        base_url: "https://portal.intake.test",
        ttl: 3600,
        clock: -> { now + 3601 }
      )
      expect { later.verify(token) }.to raise_error(DocIntake::Part7::InvalidUploadLink, /expired/)
    end
  end
end
