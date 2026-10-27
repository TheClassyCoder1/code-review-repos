# frozen_string_literal: true

require "digest"
require "net/smtp"

module DocIntake
  module Part8
    module Watches
      class ActivationRequestFailed < StandardError
        attr_reader :recipients

        def initialize(recipients)
          @recipients = recipients
          super("activation e-mail could not be sent")
        end
      end

      # Base class for events applicants and reviewers can subscribe to
      # (pack completed, document rejected, ...). Subclasses set event_type and
      # optionally content_type; the shared repository and mailer are wired at
      # boot.
      class PackEvent
        # Letters that can't be mistaken for other letters or numbers in most
        # fonts, in case people try to type these.
        DISTINGUISHABLE_LETTERS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRTUVWXYZ"

        class << self
          attr_accessor :event_type, :content_type, :repository, :mailer, :confirm_anonymous_watches

          # Start notifying the given user id or e-mail address when this
          # event occurs and meets the criteria given in +filters+.
          #
          # Returns the created (or the existing matching) watch. Anonymous
          # watches get an activation e-mail when confirm_anonymous_watches is
          # set; if that e-mail fails, ActivationRequestFailed is raised.
          def notify(user_or_email, object_id = nil, **filters)
            # Test-for-existence-then-create race: de-duplication on fire and
            # deletion of all matches on stop_notifying nullify its effects.
            watch = watches_belonging_to_user(user_or_email, object_id: object_id, **filters).first
            if watch.nil?
              create_attrs = {}
              create_attrs[:content_type] = content_type if content_type
              create_attrs[user_or_email.is_a?(String) ? :email : :user_id] = user_or_email
              secret = Array.new(10) { DISTINGUISHABLE_LETTERS[rand(DISTINGUISHABLE_LETTERS.length)] }.join
              # Registered users don't need to confirm, but anonymous users do.
              is_active = create_attrs.key?(:user_id) || !confirm_anonymous_watches
              create_attrs[:object_id] = object_id if object_id
              watch = repository.create_watch(
                secret: secret, is_active: is_active, event_type: event_type, **create_attrs
              )
              filters.each do |name, value|
                repository.create_filter(watch_id: watch[:id], name: name.to_s, value: hash_to_unsigned(value))
              end
            end

            # Send e-mail for inactive watches.
            unless watch[:is_active]
              email = watch[:user_id] ? repository.user_email(watch[:user_id]) : watch[:email]
              message = activation_email(watch, email)
              begin
                mailer.deliver(message)
              rescue Net::SMTPError
                repository.delete_watch(watch[:id])
                raise ActivationRequestFailed, [email]
              end
            end
            watch
          end

          private

          def watches_belonging_to_user(user_or_email, object_id: nil, **filters)
            key = user_or_email.is_a?(String) ? :email : :user_id
            repository.matching_watches(
              event_type: event_type,
              content_type: content_type,
              key => user_or_email,
              object_id: object_id,
              filters: filters.transform_values { |v| hash_to_unsigned(v) }
            )
          end

          def activation_email(watch, email)
            {
              to: email,
              subject: "Confirm your #{event_type} notifications",
              body: "Confirm: /portal/watches/#{watch[:id]}/activate?secret=#{watch[:secret]}"
            }
          end

          def hash_to_unsigned(value)
            Digest::SHA1.hexdigest(value.to_s)[0, 8].to_i(16)
          end
        end
      end
    end
  end
end
