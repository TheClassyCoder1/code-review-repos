# frozen_string_literal: true

require "json"
require "net/http"
require "uri"

module DocIntake
  module Part8
    module Chat
      class TranslateError < StandardError; end

      # "/translate" command for the reviewer chat: translates a borrower's
      # free-text note so reviewers can read packs filed in other languages.
      module TranslateCommand
        API_URL = "https://translation.internal.test/language/translate/v2"

        HELP_TEXT = <<~TEXT
          Translate text with the translation service.
          Usage: /translate "<text>" <target language> [<source language>]
        TEXT

        LANGUAGE_NOT_FOUND_TEXT = "%s language not found. Visit the translation service docs for supported languages."

        module_function

        def translate_bot_response(message_content, config, author, all_languages)
          message_content = message_content.strip
          return HELP_TEXT if message_content == "help" || message_content.nil? || !message_content.start_with?('"')

          split_text = rsplit_once(message_content, '" ')
          return HELP_TEXT if split_text.length == 1

          split_text.concat(split_text.delete_at(1).split(" "))
          # There is no source language
          split_text << "" if split_text.length == 2
          return HELP_TEXT if split_text.length != 3

          text_to_translate, target_language, source_language = split_text
          text_to_translate = text_to_translate[1..]
          target_language = code_for_language(target_language, all_languages)
          return format(LANGUAGE_NOT_FOUND_TEXT, "Target") if target_language == ""

          if source_language != ""
            source_language = code_for_language(source_language, all_languages)
            return format(LANGUAGE_NOT_FOUND_TEXT, "Source") if source_language == ""
          end

          begin
            translated_text = translate(text_to_translate, config.fetch("key"), target_language, source_language)
          rescue SocketError, SystemCallError, Net::OpenTimeout => conn_err
            return "Could not connect to the translation service. #{conn_err}."
          rescue TranslateError => tr_err
            return "Translate Error. #{tr_err}."
          rescue StandardError => err
            return "Error. #{err}."
          end
          "#{translated_text} (from #{author})"
        end

        def translate(text, key, target, source)
          uri = URI(API_URL)
          query = { "q" => text, "target" => target, "format" => "text", "key" => key }
          query["source"] = source unless source.empty?
          uri.query = URI.encode_www_form(query)
          response = Net::HTTP.get_response(uri)
          body = JSON.parse(response.body)
          raise TranslateError, body.dig("error", "message") unless response.is_a?(Net::HTTPSuccess)

          body.dig("data", "translations", 0, "translatedText")
        end

        def code_for_language(language, all_languages)
          entry = all_languages.find do |lang|
            lang["name"].to_s.casecmp?(language) || lang["language"].to_s.casecmp?(language)
          end
          entry ? entry["language"] : ""
        end

        def rsplit_once(text, separator)
          index = text.rindex(separator)
          return [text] if index.nil?

          [text[0...index], text[(index + separator.length)..]]
        end
      end
    end
  end
end
