using System.Runtime.Serialization.Formatters.Binary;
using System.Text.Json;
using PolicyAdmin.Models;

namespace PolicyAdmin.Services;

public sealed class EndorsementImportService
{
    private static readonly JsonSerializerOptions JsonOptions = new() { PropertyNameCaseInsensitive = true };

    private readonly ILogger<EndorsementImportService> _logger;

    public EndorsementImportService(ILogger<EndorsementImportService> logger)
    {
        _logger = logger;
    }

#pragma warning disable SYSLIB0011
    public IReadOnlyList<Endorsement> ReadLegacyBatch(Stream upload)
    {
        var formatter = new BinaryFormatter();
        var batch = (List<Endorsement>)formatter.Deserialize(upload);
        _logger.LogInformation("Read {Count} endorsements from legacy batch", batch.Count);
        return batch;
    }
#pragma warning restore SYSLIB0011

    public static string ShortTypeName(string typeName) =>
        typeName.Contains('.') ? typeName[(typeName.LastIndexOf('.') + 1)..] : typeName;

    public static bool IsKnownPrefix(string typeName) => typeName.StartsWith("PolicyAdmin.Services.", StringComparison.Ordinal);

    public object ReadTypedChange(EndorsementImportEnvelope envelope)
    {
        var targetType = Type.GetType(envelope.TypeName, throwOnError: true)!;
        var change = JsonSerializer.Deserialize(envelope.Payload, targetType, JsonOptions)
            ?? throw new InvalidDataException("Endorsement payload was empty.");
        _logger.LogInformation("Read typed endorsement change {Type}", ShortTypeName(envelope.TypeName));
        return change;
    }

    public static IReadOnlyList<string> SummarizeTypes(IEnumerable<EndorsementImportEnvelope> envelopes) =>
        envelopes.Select(e => ShortTypeName(e.TypeName)).Distinct().OrderBy(n => n).ToList();

    public static int CountKnown(IEnumerable<EndorsementImportEnvelope> envelopes) =>
        envelopes.Count(e => EndorsementTypeRegistry.IsAllowed(e.TypeName));

    public static IReadOnlyList<string> ValidateEffectiveDates(Policy policy, IEnumerable<Endorsement> endorsements)
    {
        var errors = new List<string>();
        foreach (var endorsement in endorsements)
        {
            if (endorsement.EffectiveDate < policy.EffectiveDate || endorsement.EffectiveDate >= policy.ExpirationDate)
            {
                errors.Add($"Endorsement {endorsement.Code} is effective {endorsement.EffectiveDate:yyyy-MM-dd}, outside the policy term.");
            }
            if (string.IsNullOrWhiteSpace(endorsement.Description))
            {
                errors.Add($"Endorsement {endorsement.Code} needs a description.");
            }
        }
        return errors;
    }
}
