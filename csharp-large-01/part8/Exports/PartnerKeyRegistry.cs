using System.Text.RegularExpressions;

namespace Lending.Part8.Exports;

/// <summary>
/// Maps partner codes to the OpenPGP fingerprints configured under Exports:PartnerKeys.
/// Only full 40-hex-digit fingerprints are accepted; anything else fails at startup.
/// </summary>
public sealed partial class PartnerKeyRegistry
{
    private readonly IReadOnlyDictionary<string, string> _fingerprints;

    public PartnerKeyRegistry(IConfiguration configuration)
    {
        var fingerprints = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var entry in configuration.GetSection("Exports:PartnerKeys").GetChildren())
        {
            var fingerprint = (entry.Value ?? string.Empty).Replace(" ", string.Empty).ToUpperInvariant();
            if (!FingerprintPattern().IsMatch(fingerprint))
            {
                throw new InvalidOperationException($"Exports:PartnerKeys:{entry.Key} is not a 40-digit fingerprint.");
            }
            fingerprints[entry.Key] = fingerprint;
        }
        _fingerprints = fingerprints;
    }

    public string ResolveFingerprint(string partnerCode) =>
        _fingerprints.TryGetValue(partnerCode, out var fingerprint)
            ? fingerprint
            : throw new KeyNotFoundException($"No export key registered for partner {partnerCode}.");

    [GeneratedRegex("^[0-9A-F]{40}$")]
    private static partial Regex FingerprintPattern();
}
