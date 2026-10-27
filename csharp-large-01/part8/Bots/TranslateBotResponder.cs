namespace Lending.Part8.Bots;

public sealed class TranslateException(string message) : Exception(message);

public interface ITranslator
{
    string Translate(string text, string apiKey, string targetLanguage, string sourceLanguage);
}

public sealed class TranslateBotResponder(ITranslator translator)
{
    private const string HelpText = "Usage: @translate \"<text>\" <target language> [<source language>]";
    private const string LanguageNotFoundText = "{0} language not found. Visit the translate docs for supported languages.";

    public string GetTranslateBotResponse(
        string? messageContent, IReadOnlyDictionary<string, string> configFile, string author, IReadOnlyList<(string Name, string Code)> allLanguages)
    {
        messageContent = messageContent?.Trim();
        if (messageContent == "help" || messageContent is null || !messageContent.StartsWith('"'))
        {
            return HelpText;
        }
        var cut = messageContent.LastIndexOf("\" ", StringComparison.Ordinal);
        if (cut < 0)
        {
            return HelpText;
        }
        var splitText = new List<string> { messageContent[..cut] };
        splitText.AddRange(messageContent[(cut + 2)..].Split(' '));
        if (splitText.Count == 2)
        {
            // There is no source language
            splitText.Add("");
        }
        if (splitText.Count != 3)
        {
            return HelpText;
        }
        var textToTranslate = splitText[0][1..];
        var targetLanguage = GetCodeForLanguage(splitText[1], allLanguages);
        var sourceLanguage = splitText[2];
        if (targetLanguage == "")
        {
            return string.Format(LanguageNotFoundText, "Target");
        }
        if (sourceLanguage != "")
        {
            sourceLanguage = GetCodeForLanguage(sourceLanguage, allLanguages);
            if (sourceLanguage == "")
            {
                return string.Format(LanguageNotFoundText, "Source");
            }
        }
        string translatedText;
        try
        {
            translatedText = translator.Translate(textToTranslate, configFile["key"], targetLanguage, sourceLanguage);
        }
        catch (HttpRequestException connErr)
        {
            return $"Could not connect to Google Translate. {connErr.Message}.";
        }
        catch (TranslateException trErr)
        {
            return $"Translate Error. {trErr.Message}.";
        }
        catch (Exception err)
        {
            return $"Error. {err.Message}.";
        }
        return $"{translatedText} (from {author})";
    }

    private static string GetCodeForLanguage(string language, IReadOnlyList<(string Name, string Code)> allLanguages)
    {
        foreach (var (name, code) in allLanguages)
        {
            if (string.Equals(name, language, StringComparison.OrdinalIgnoreCase)
                || string.Equals(code, language, StringComparison.OrdinalIgnoreCase))
            {
                return code;
            }
        }
        return "";
    }
}
