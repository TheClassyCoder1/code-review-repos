using System.Text.Json;

namespace Lending.Servicing.Part4.Bureau;

public static class BureauResponseParser
{
    public const int MinScore = 300;
    public const int MaxScore = 850;

    /// <summary>
    /// Extracts the score from a bureau payload of the form
    /// <c>{"result":{"score":712,"model":"FICO8"}}</c>. Older responses put the
    /// score at the top level, so both shapes are accepted.
    /// </summary>
    public static int ParseScore(string body)
    {
        if (string.IsNullOrWhiteSpace(body))
        {
            throw new FormatException("Bureau response body is empty.");
        }

        using var document = JsonDocument.Parse(body);
        var root = document.RootElement;
        if (root.ValueKind != JsonValueKind.Object)
        {
            throw new FormatException("Bureau response is not a JSON object.");
        }

        var container = root.TryGetProperty("result", out var result) && result.ValueKind == JsonValueKind.Object
            ? result
            : root;

        if (!container.TryGetProperty("score", out var scoreElement)
            || scoreElement.ValueKind != JsonValueKind.Number
            || !scoreElement.TryGetInt32(out var score))
        {
            throw new FormatException("Bureau response does not contain an integer score.");
        }

        if (score < MinScore || score > MaxScore)
        {
            throw new FormatException($"Bureau score {score} is outside the {MinScore}-{MaxScore} range.");
        }

        return score;
    }
}
