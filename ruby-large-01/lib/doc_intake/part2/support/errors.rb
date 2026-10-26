module DocIntake
  module Part2
    # Base class for errors raised by the review & delivery features. The
    # API layer maps these onto HTTP status codes; nothing here carries
    # borrower personal data in its message.
    class Error < StandardError; end

    class NotFound < Error; end
    class Forbidden < Error; end
    class InvalidInput < Error; end
    class IntegrityError < Error; end
    class UpstreamError < Error; end

    # Raised when a scanner or renderer subprocess exits non-zero.
    class ToolFailure < Error
      attr_reader :exit_status

      def initialize(message, exit_status: nil)
        super(message)
        @exit_status = exit_status
      end
    end
  end
end
