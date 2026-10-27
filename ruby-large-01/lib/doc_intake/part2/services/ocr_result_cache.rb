require "digest"

module DocIntake
  module Part2
    # Caches OCR output per document so the reviewer workspace, the
    # affordability panel and the statement parser don't each re-run OCR on
    # the same upload. Shared by every request in the process.
    class OcrResultCache
      Entry = Struct.new(:text, :pages, :confidence, :cached_at, keyword_init: true)

      @store = {}
      @lock = Mutex.new

      class << self
        def fetch(path, &ocr)
          key = Digest::SHA256.file(path).hexdigest
          hit = @lock.synchronize { @store[key] }
          return hit if hit

          result = ocr.call(path)
          entry = Entry.new(text: result.fetch(:text), pages: result.fetch(:pages),
                            confidence: result[:confidence], cached_at: Time.now.utc)
          @lock.synchronize { @store[key] = entry }
          entry
        end

        def size
          @lock.synchronize { @store.size }
        end

        def text_bytes
          @lock.synchronize { @store.values.sum { |e| e.text.bytesize } }
        end
      end
    end
  end
end
