require "rspec"
require "tmpdir"
require "stringio"
require_relative "../../lib/doc_intake/part7/upload_inspector"
require_relative "../../lib/doc_intake/part7/replacement_store"

RSpec.describe DocIntake::Part7::ReplacementStore do
  around do |example|
    Dir.mktmpdir("intake-store") do |dir|
      @root = dir
      example.run
    end
  end

  let(:store) { described_class.new(@root) }
  let(:inspected) do
    DocIntake::Part7::UploadInspector::Result.new(
      filename: "payslip.pdf", content_type: "application/pdf", size: 8, sha256: "abc"
    )
  end

  it "writes the file under the pack and request directory" do
    relative = store.store(pack_id: 12, request_id: 3, inspected: inspected, io: StringIO.new("%PDF-1.4"))
    expect(relative).to match(%r{\Apacks/12/resubmissions/3/[0-9a-f-]{36}-payslip\.pdf\z})
    expect(File.read(store.absolute(relative))).to eq("%PDF-1.4")
  end

  it "leaves no partial files behind" do
    store.store(pack_id: 12, request_id: 3, inspected: inspected, io: StringIO.new("%PDF-1.4"))
    expect(Dir.glob(File.join(@root, "**", "*.partial"))).to be_empty
  end

  it "refuses paths outside the root" do
    expect { store.absolute("../../etc/passwd") }.to raise_error(ArgumentError)
  end

  it "rejects non-numeric pack ids" do
    expect do
      store.store(pack_id: "../x", request_id: 3, inspected: inspected, io: StringIO.new("x"))
    end.to raise_error(ArgumentError)
  end

  it "removes stored files" do
    relative = store.store(pack_id: 1, request_id: 1, inspected: inspected, io: StringIO.new("%PDF"))
    store.remove(relative)
    expect(File.exist?(store.absolute(relative))).to be(false)
  end
end
