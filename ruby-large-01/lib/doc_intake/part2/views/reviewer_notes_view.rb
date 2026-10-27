module DocIntake
  module Part2
    # Renders the notes thread shown beside a pack in the reviewer
    # workspace. Notes are listed newest first with the reviewer's display
    # handle and a relative timestamp.
    class ReviewerNotesView
      def initialize(notes, now: Time.now.utc)
        @notes = notes
        @now = now
      end

      def render
        return %(<section class="notes notes--empty"><p>No notes yet.</p></section>) if @notes.empty?

        items = @notes.sort_by { |n| n[:created_at] }.reverse.map { |note| render_note(note) }
        %(<section class="notes">\n#{items.join("\n")}\n</section>)
      end

      private

      def render_note(note)
        body = NoteFormatter.to_html(note[:body])
        handle = Html.escape(note[:reviewer_handle])
        <<~HTML.chomp
          <article class="note" data-note-id="#{Html.escape_attr(note[:id])}">
            <header><span class="note__author">#{handle}</span> <time>#{relative(note[:created_at])}</time></header>
            <div class="note__body">#{body}</div>
          </article>
        HTML
      end

      def relative(time)
        return "" if time.nil?

        seconds = (@now - time).to_i
        case seconds
        when 0...60 then "just now"
        when 60...3600 then "#{seconds / 60}m ago"
        when 3600...86_400 then "#{seconds / 3600}h ago"
        else time.strftime("%d %b %Y")
        end
      end
    end
  end
end
