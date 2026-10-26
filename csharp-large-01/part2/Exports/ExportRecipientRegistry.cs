using System.Diagnostics.CodeAnalysis;
using System.Text.RegularExpressions;

namespace Lending.Part2.Exports;

/// <summary>
/// Maps partner codes to the OpenPGP key fingerprints their exports are encrypted for.
/// Only fingerprints from configuration are ever handed to the encryptor, and every
/// configured value must be a bare 40-character hex fingerprint.
/// </summary>
public sealed class ExportRecipientRegistry
{
    private static readonly Regex FingerprintPattern = new("^[A-F0-9]{40}$", RegexOptions.Compiled | RegexOptions.CultureInvariant);

    private readonly IReadOnlyDictionary<string, string> _fingerprints;

    public ExportRecipientRegistry(IConfiguration configuration)
    {
        var map = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var partner in configuration.GetSection("Exports:Recipients").GetChildren())
        {
            var fingerprint = partner.Value?.Replace(" ", string.Empty, StringComparison.Ordinal).ToUpperInvariant();
            if (fingerprint is null || !FingerprintPattern.IsMatch(fingerprint))
            {
                throw new InvalidOperationException($"Export recipient '{partner.Key}' has an invalid key fingerprint.");
            }
            map[partner.Key] = fingerprint;
        }
        _fingerprints = map;
    }

    public IEnumerable<string> PartnerCodes => _fingerprints.Keys;

    public bool TryResolve(string partnerCode, [NotNullWhen(true)] out string? fingerprint)
    {
        return _fingerprints.TryGetValue(partnerCode, out fingerprint);
    }
}
