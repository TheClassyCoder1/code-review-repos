namespace Lending.Servicing.Part3.Infrastructure;

/// <summary>
/// Builds the flat settings map handed to the servicing integrations. Every key the
/// integrations read is guaranteed to be present after <see cref="Build"/>.
/// </summary>
public static class SettingsDefaults
{
    public const string BureauHost = "bureau.host";
    public const string BureauPort = "bureau.port";
    public const string BureauProductCode = "bureau.product";
    public const string BureauReadTimeoutMs = "bureau.read-timeout-ms";

    private static readonly IReadOnlyDictionary<string, string> Defaults = new Dictionary<string, string>
    {
        [BureauHost] = "bureau-gw.internal",
        [BureauPort] = "7015",
        [BureauProductCode] = "SVC-SCORE-V2",
        [BureauReadTimeoutMs] = "5000"
    };

    public static IReadOnlyDictionary<string, string> Build(IReadOnlyDictionary<string, string?> overrides)
    {
        var merged = new Dictionary<string, string>(Defaults, StringComparer.Ordinal);

        foreach (var (key, value) in overrides)
        {
            if (!string.IsNullOrWhiteSpace(value))
            {
                merged[key] = value.Trim();
            }
        }

        if (!int.TryParse(merged[BureauPort], out var port) || port is <= 0 or > 65535)
        {
            merged[BureauPort] = Defaults[BureauPort];
        }

        if (!int.TryParse(merged[BureauReadTimeoutMs], out var timeout) || timeout <= 0)
        {
            merged[BureauReadTimeoutMs] = Defaults[BureauReadTimeoutMs];
        }

        return merged;
    }
}
