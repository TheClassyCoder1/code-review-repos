namespace PolicyAdmin.Services;

public static class EndorsementTypeRegistry
{
    private static readonly HashSet<string> AllowedTypeNames = new(StringComparer.Ordinal)
    {
        "PolicyAdmin.Services.AddDriverEndorsement",
        "PolicyAdmin.Services.ChangeAddressEndorsement",
        "PolicyAdmin.Services.CoverageLimitEndorsement"
    };

    public static bool IsAllowed(string? typeName) => typeName is not null && AllowedTypeNames.Contains(typeName);
}

public sealed record AddDriverEndorsement(string DriverName, DateOnly DateOfBirth, string LicenseNumber);

public sealed record ChangeAddressEndorsement(string Street, string City, string StateCode, string PostalCode);

public sealed record CoverageLimitEndorsement(string CoverageCode, decimal NewLimit, decimal NewDeductible);
