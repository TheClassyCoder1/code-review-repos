require "cgi"

module DocIntake
  module Part2
    # Small HTML helpers shared by the portal and reviewer views.
    module Html
      module_function

      def escape(value)
        CGI.escapeHTML(value.to_s)
      end

      def escape_attr(value)
        escape(value).gsub("`", "&#96;")
      end

      def tag(name, content, attrs = {})
        rendered = attrs.map { |k, v| %( #{k}="#{escape_attr(v)}") }.join
        "<#{name}#{rendered}>#{content}</#{name}>"
      end

      def format_bytes(bytes)
        return "0 B" if bytes.nil? || bytes.zero?

        units = %w[B KB MB GB]
        exp = [(Math.log(bytes) / Math.log(1024)).floor, units.size - 1].min
        format("%.1f %s", bytes.to_f / (1024**exp), units[exp])
      end
    end
  end
end
