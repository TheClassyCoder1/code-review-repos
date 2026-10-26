require "rspec"
require "tempfile"
require "digest"
require_relative "../../lib/doc_intake/part7/upload_inspector"

RSpec.describe DocIntake::Part7::UploadInspector do
  let(:inspector) { described_class.new(max_bytes: 1024) }

  def upload_with(content, type: "application/pdf", filename: "statement.pdf")
    file = Tempfile.new("upload")
    file.binmode
    file.write(content)
    file.rewind
    { filename: filename, type: type, tempfile: file }
  end

  it "accepts a pdf and returns its digest" do
    content = "%PDF-1.7\nbody"
    result = inspector.inspect_upload(upload_with(content))
    expect(result.content_type).to eq("application/pdf")
    expect(result.sha256).to eq(Digest::SHA256.hexdigest(content))
    expect(result.size).to eq(content.bytesize)
  end

  it "rejects unsupported content types" do
    expect do
      inspector.inspect_upload(upload_with("GIF89a", type: "image/gif", filename: "a.gif"))
    end.to raise_error(DocIntake::Part7::UploadRejected) { |e| expect(e.code).to eq("unsupported_type") }
  end

  it "rejects content that does not match the declared type" do
    expect do
      inspector.inspect_upload(upload_with("MZ\x90\x00", type: "application/pdf"))
    end.to raise_error(DocIntake::Part7::UploadRejected) { |e| expect(e.code).to eq("type_mismatch") }
  end

  it "rejects a checksum mismatch" do
    expect do
      inspector.inspect_upload(upload_with("%PDF-1.4"), declared_sha256: "0" * 64)
    end.to raise_error(DocIntake::Part7::UploadRejected) { |e| expect(e.code).to eq("checksum_mismatch") }
  end

  it "accepts a matching upper-case checksum" do
    content = "%PDF-1.4 ok"
    declared = Digest::SHA256.hexdigest(content).upcase
    expect { inspector.inspect_upload(upload_with(content), declared_sha256: declared) }.not_to raise_error
  end

  it "strips directories from the filename" do
    result = inspector.inspect_upload(upload_with("%PDF-1.4", filename: "..\\..\\etc\\statement.pdf"))
    expect(result.filename).to eq("statement.pdf")
  end

  it "rejects filenames with control characters" do
    expect do
      inspector.inspect_upload(upload_with("%PDF-1.4", filename: "bad\nname.pdf"))
    end.to raise_error(DocIntake::Part7::UploadRejected) { |e| expect(e.code).to eq("bad_filename") }
  end

  it "rejects a request without a file" do
    expect { inspector.inspect_upload(nil) }.to raise_error(DocIntake::Part7::UploadRejected)
  end
end
