require "yaml"

module Newsletter
  module PublicationDefaults
    module_function

    def load(path)
      settings = YAML.unsafe_load(File.read(path)) || {}
      settings.transform_keys(&:to_s).slice("sender_name", "reply_to", "layout", "double_opt_in", "footer_html")
    end
  end
end
