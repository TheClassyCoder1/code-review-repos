using System.ComponentModel.DataAnnotations;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace PolicyAdmin.Models;

public enum PolicyStatus
{
    Quoted,
    Bound,
    InForce,
    PendingCancellation,
    Cancelled,
    Expired
}

public enum LineOfBusiness
{
    Auto,
    Homeowners,
    Commercial,
    Umbrella
}

public sealed class Policy
{
    public int Id { get; set; }
    public string PolicyNumber { get; set; } = "";
    public int AgentId { get; set; }
    public string InsuredName { get; set; } = "";
    public LineOfBusiness Line { get; set; }
    public PolicyStatus Status { get; set; }
    public DateOnly EffectiveDate { get; set; }
    public DateOnly ExpirationDate { get; set; }
    public decimal AnnualPremium { get; set; }
    public string? UnderwritingNotes { get; set; }
}

[Serializable]
public sealed class Endorsement
{
    public int Id { get; set; }
    public int PolicyId { get; set; }
    public string Code { get; set; } = "";
    public string Description { get; set; } = "";
    public DateOnly EffectiveDate { get; set; }
    public decimal PremiumDelta { get; set; }
    public int CreatedByAgentId { get; set; }
}

public sealed class Agent
{
    public int Id { get; set; }
    public string Email { get; set; } = "";
    public string DisplayName { get; set; } = "";
    public string PasswordHash { get; set; } = "";
    public string LicenseNumber { get; set; } = "";
    public DateOnly LicenseExpires { get; set; }
    public decimal CommissionRate { get; set; }
}

public sealed class Quote
{
    public string Reference { get; set; } = "";
    public int AgentId { get; set; }
    public LineOfBusiness Line { get; set; }
    public decimal QuotedPremium { get; set; }
    public DateTime CreatedUtc { get; set; }
}

[JsonConverter(typeof(MaskedValueJsonConverter))]
public readonly struct MaskedValue
{
    private readonly string? _raw;

    public MaskedValue(string raw) => _raw = raw ?? "";

    public string Reveal() => _raw ?? "";

    public override string ToString() => Reveal().Length <= 4 ? "****" : new string('*', Reveal().Length - 4) + Reveal()[^4..];
}

public sealed class MaskedValueJsonConverter : JsonConverter<MaskedValue>
{
    public override MaskedValue Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options) =>
        new(reader.GetString() ?? "");

    public override void Write(Utf8JsonWriter writer, MaskedValue value, JsonSerializerOptions options) =>
        writer.WriteStringValue(value.ToString());
}

public sealed class PremiumPayment
{
    public int PolicyId { get; set; }
    public decimal Amount { get; set; }
    public MaskedValue AccountNumber { get; set; }
    public MaskedValue RoutingNumber { get; set; }
    public string PayerName { get; set; } = "";
}

public sealed class LoginRequest
{
    [Required, EmailAddress]
    public string Email { get; set; } = "";

    [Required]
    public string Password { get; set; } = "";
}

public sealed class ApproveEndorsementRequest
{
    [Required]
    public int EndorsementId { get; set; }

    [RegularExpression(@"^/(?![/\\]).*$", ErrorMessage = "Return path must be a local path.")]
    public string? ReturnPath { get; set; }
}

public sealed class CarrierSubmission
{
    public string PolicyNumber { get; set; } = "";
    public string CarrierCallbackUrl { get; set; } = "";
    public decimal Premium { get; set; }
}

public sealed record EndorsementImportEnvelope(string TypeName, string Payload);

public sealed record PremiumBreakdown(decimal Base, decimal EndorsementTotal, decimal Taxes, decimal Total);
