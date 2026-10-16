using PolicyAdmin.Models;

namespace PolicyAdmin.Services;

public sealed class QuoteService
{
    private const string ReferenceAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static readonly IReadOnlyDictionary<LineOfBusiness, decimal> LineFactors = new Dictionary<LineOfBusiness, decimal>
    {
        [LineOfBusiness.Auto] = 1.00m,
        [LineOfBusiness.Homeowners] = 0.85m,
        [LineOfBusiness.Commercial] = 1.40m,
        [LineOfBusiness.Umbrella] = 0.35m
    };

    public string CreateQuoteReference()
    {
        var chars = new char[8];
        for (var i = 0; i < chars.Length; i++)
        {
            chars[i] = ReferenceAlphabet[Random.Shared.Next(ReferenceAlphabet.Length)];
        }
        return "Q-" + new string(chars);
    }

    public static bool LooksLikeQuoteReference(string? value) =>
        value is { Length: 10 } && value.StartsWith("Q-", StringComparison.Ordinal);

    public static decimal LineFactor(LineOfBusiness line) => LineFactors[line];

    public Quote PriceQuote(int agentId, LineOfBusiness line, decimal baseRate, int priorClaims)
    {
        var surcharge = 1m + Math.Min(priorClaims, 5) * 0.10m;
        var premium = decimal.Round(baseRate * LineFactors[line] * surcharge, 2, MidpointRounding.AwayFromZero);
        return new Quote
        {
            Reference = CreateQuoteReference(),
            AgentId = agentId,
            Line = line,
            QuotedPremium = premium,
            CreatedUtc = DateTime.UtcNow
        };
    }
}
