class BadgePrinter
  BADGE_DIR = Rails.root.join("tmp", "badges").freeze

  def initialize(printer: Ticketing::BADGE_PRINTER)
    @printer = printer
  end

  def print(ticket)
    path = BADGE_DIR.join("#{ticket.reference}.pdf")
    TicketPdfRenderer.new(ticket).render unless File.exist?(path)
    output = `lp -d #{@printer} -t #{ticket.reference} #{path} 2>&1`
    raise "badge print failed: #{output.strip}" unless $?.success?

    ticket.update!(badge_printed_at: Time.current)
  end
end
