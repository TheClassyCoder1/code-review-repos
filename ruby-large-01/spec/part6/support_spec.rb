# frozen_string_literal: true

require "date"
require_relative "../../lib/doc_intake/part6/support/pii_mask"
require_relative "../../lib/doc_intake/part6/support/date_format"
require_relative "../../lib/doc_intake/part6/support/money"
require_relative "../../lib/doc_intake/part6/support/analytics_allowlist"

RSpec.describe "part6 support helpers" do
  describe DocIntake::Part6::Support::PiiMask do
    it "keeps only the first letters and the top-level domain of an email" do
      expect(described_class.email("jane.doe@example.com")).to eq("j***@e***.com")
    end

    it "masks blank and malformed emails entirely" do
      expect(described_class.email(nil)).to eq("***")
      expect(described_class.email("not-an-email")).to eq("***")
    end

    it "keeps the last four digits of a phone number" do
      expect(described_class.phone("+44 7700 900123")).to eq("***0123")
    end

    it "keeps the last two characters of a national id" do
      expect(described_class.national_id("QQ 12 34 56 C")).to eq("***6C")
    end
  end

  describe DocIntake::Part6::Support::DateFormat do
    let(:day) { Date.new(2026, 10, 26) }

    it "renders ISO dates" do
      expect(described_class.iso_date(day)).to eq("2026-10-26")
      expect(described_class.iso_date(nil)).to be_nil
    end

    it "renders borrower-facing dates" do
      expect(described_class.display_date("2026-10-26")).to eq("26 Oct 2026")
    end

    it "renders UTC timestamps" do
      expect(described_class.iso_timestamp(Time.utc(2026, 10, 26, 9, 5, 7))).to eq("2026-10-26T09:05:07Z")
    end
  end

  describe DocIntake::Part6::Support::Money do
    it "rounds ledger amounts half-up" do
      expect(described_class.round_cents(BigDecimal("1.375")).to_s("F")).to eq("1.38")
      expect(described_class.round_cents(BigDecimal("1.374")).to_s("F")).to eq("1.37")
    end

    it "rounds verification fees up to the next cent" do
      expect(described_class.round_fee_cents(BigDecimal("1.371")).to_s("F")).to eq("1.38")
      expect(described_class.round_fee_cents(BigDecimal("2.125")).to_s("F")).to eq("2.13")
    end

    it "formats with two decimals" do
      expect(described_class.format(BigDecimal("4.5"))).to eq("GBP 4.50")
    end

    it "rejects non-finite floats" do
      expect { described_class.to_decimal(Float::NAN) }.to raise_error(ArgumentError)
    end
  end

  describe DocIntake::Part6::Support::AnalyticsAllowlist do
    it "keeps internal ids and known enums" do
      result = described_class.filter(application_id: 42, check_id: "3f1c2a9e-0b7d-4c1e-9a55-1c2d3e4f5a6b",
                                      channel: "sms")
      expect(result.properties.keys).to contain_exactly("application_id", "check_id", "channel")
      expect(result.dropped_keys).to be_empty
    end

    it "drops anything that is not an id or enum" do
      result = described_class.filter(application_id: 42, email: "jane@example.com", user_id: "jane@example.com")
      expect(result.properties).to eq("application_id" => 42)
      expect(result.dropped_keys).to contain_exactly("email", "user_id")
    end
  end
end
