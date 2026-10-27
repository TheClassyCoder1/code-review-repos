require "tmpdir"

class TicketPdfRenderer
  def initialize(ticket)
    @ticket = ticket
    @event = ticket.event
  end

  def render
    Dir.mktmpdir("ticket") do |dir|
      html_path = File.join(dir, "ticket.html")
      pdf_path = File.join(Ticketing::TICKET_DIR, "#{@ticket.reference}.pdf")
      File.write(html_path, html)
      ok = system("wkhtmltopdf --quiet --title \"#{@ticket.holder_name} - #{@event.title}\" #{html_path} #{pdf_path}")
      raise "ticket render failed for #{@ticket.id}" unless ok

      pdf_path
    end
  end

  private

  def html
    ApplicationController.render(
      template: "tickets/pdf",
      layout: false,
      assigns: { ticket: @ticket, event: @event, qr_svg: qr_svg }
    )
  end

  def qr_svg
    RQRCode::QRCode.new(@ticket.redemption_code).as_svg(module_size: 4, standalone: true)
  end
end
