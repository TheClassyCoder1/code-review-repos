# frozen_string_literal: true

require "erb"
require "net/smtp"

module DocIntake
  module Part8
    module Notifications
      # Sends review-workflow notifications (pack submitted, pack approved,
      # pack rejected) to reviewer accounts that opted in to them.
      class ReviewerNotificationMailer
        TEMPLATE_ROOT = File.expand_path("templates", __dir__)

        # +profiles+ answers preference lookups for a reviewer,
        # +smtp+ is a Net::SMTP factory: -> { Net::SMTP.new(host, port) }.
        def initialize(profiles:, smtp:, settings:, logger:, from_address:)
          @profiles = profiles
          @smtp = smtp
          @settings = settings
          @logger = logger
          @from_address = from_address
        end

        def send_notification(recipient_users, notification, extra_context)
          # Get list of e-mail addresses
          email_recipients = recipient_users.select do |recipient|
            recipient.active? &&
              !recipient.email.to_s.empty? &&
              @profiles.for_user(recipient).public_send("#{notification}_notifications")
          end

          # Return if there are no e-mail addresses
          return true if email_recipients.empty?

          # Get template
          template_subject = "notifications/#{notification}_subject.txt"
          template_text = "notifications/#{notification}.txt"
          template_html = "notifications/#{notification}.html"

          # Common context to template
          context = { settings: @settings }
          context.merge!(extra_context)

          sent_count = 0
          @smtp.call.start do |open_connection|
            email_recipients.each do |recipient|
              # update context with this recipient
              context[:user] = recipient

              # Render in the recipient's preferred language
              email_subject, email_content = with_locale(@profiles.for_user(recipient).preferred_language) do |locale|
                [render_to_string(template_subject, context, locale).strip,
                 render_to_string(template_text, context, locale).strip]
              end

              html_message = nil
              html_message = render_to_string(template_html, context, nil) if @settings.notification_use_html

              begin
                # Send e-mail
                send_mail(open_connection, email_subject, email_content, [recipient.email], html_message: html_message)
                sent_count += 1
              rescue StandardError => e
                @logger.error("Failed to send notification email '#{email_subject}' to #{recipient.email}: #{e.full_message}")
              end
            end
          end

          sent_count == email_recipients.length
        end

        private

        def with_locale(locale)
          yield(locale.to_s.empty? ? "en" : locale.to_s)
        end

        def render_to_string(name, context, locale)
          localized = locale && File.join(TEMPLATE_ROOT, locale, name)
          path = localized && File.exist?(localized) ? localized : File.join(TEMPLATE_ROOT, name)
          ERB.new(File.read(path)).result_with_hash(context)
        end

        def send_mail(connection, subject, text, recipients, html_message: nil)
          body = +"From: #{@from_address}\r\nTo: #{recipients.join(', ')}\r\nSubject: #{subject}\r\n"
          if html_message
            boundary = "doc-intake-#{Time.now.to_i}"
            body << "MIME-Version: 1.0\r\nContent-Type: multipart/alternative; boundary=#{boundary}\r\n\r\n"
            body << "--#{boundary}\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n#{text}\r\n"
            body << "--#{boundary}\r\nContent-Type: text/html; charset=utf-8\r\n\r\n#{html_message}\r\n--#{boundary}--\r\n"
          else
            body << "Content-Type: text/plain; charset=utf-8\r\n\r\n#{text}\r\n"
          end
          connection.send_message(body, @from_address, recipients)
        end
      end
    end
  end
end
