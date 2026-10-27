require_relative "../../../lib/doc_intake/part5/statements/statement_parser"

RSpec.describe DocIntake::Part5::Statements::StatementParser do
  let(:parser) { described_class.new }

  let(:csv) do
    <<~CSV
      Date,Description,Amount,Balance
      2026-07-01,ACME LTD SALARY,2400.00,2500.00
      2026-07-03,ACME LTD SALARY,2500.00,5000.00
      2026-07-05,ACME LTD SALARY,-2500.00,2500.00
      2026-07-20,RENT,(2600.00),-100.00
      2026-08-01,ACME LTD SALARY,2550.00,2450.00
      2026-09-01,ACME LTD PAYROLL BONUS,400.00,2850.00
      2026-09-01,ACME LTD SALARY,2550.00,5400.00
      ,Closing balance,,5400.00
    CSV
  end

  it "skips footer rows and sorts by date" do
    rows = parser.parse_rows(csv)
    expect(rows.size).to eq(7)
    expect(rows.map(&:posted_on)).to eq(rows.map(&:posted_on).sort)
  end

  it "reports the line number of an unparseable row" do
    bad = "Date,Amount\n2026-07-01,12.00\nnot-a-date,3.00\n"
    expect { parser.parse_rows(bad) }.to raise_error(described_class::ParseError, /line 3/)
  end

  it "ignores reversed salary credits and keeps one credit per month" do
    credits = parser.detect_salary_credits(parser.parse_rows(csv))
    expect(credits.map { |c| c.posted_on.month }).to eq([7, 8, 9])
    expect(credits.first.posted_on).to eq(Date.new(2026, 7, 1))
    expect(credits.last.amount).to eq(BigDecimal("2550.00"))
  end

  it "counts overdrawn days including days without postings" do
    days = parser.overdraft_days(parser.parse_rows(csv), opening_balance: "100.00")
    expect(days).to eq(12)
  end

  it "reports month-end balances keyed by month" do
    balances = parser.monthly_balances(parser.parse_rows(csv), opening_balance: "100.00")
    expect(balances.keys).to eq(%w[2026-07 2026-08 2026-09])
    expect(balances["2026-07"]).to eq(BigDecimal("-100.00"))
  end
end
