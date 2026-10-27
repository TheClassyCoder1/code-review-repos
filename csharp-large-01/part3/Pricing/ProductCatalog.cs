namespace Lending.Servicing.Part3.Pricing;

/// <summary>
/// The fixed set of lending products the servicing module prices. New products ship with
/// a code change here, never from request data.
/// </summary>
public static class ProductCatalog
{
    public const string FallbackCode = "PL-STD";

    public static IReadOnlySet<string> KnownCodes => Codes;

    private static readonly HashSet<string> Codes = new(StringComparer.OrdinalIgnoreCase)
    {
        "PL-STD",
        "PL-PRIME",
        "AUTO-NEW",
        "AUTO-USED",
        "SME-TERM",
        "SME-REVOLVE",
        "BNPL-4"
    };

    public static string Canonicalize(string? productCode)
    {
        if (string.IsNullOrWhiteSpace(productCode))
        {
            return FallbackCode;
        }

        var trimmed = productCode.Trim();
        return Codes.TryGetValue(trimmed, out var canonical) ? canonical : FallbackCode;
    }
}
