using Lending.Part2.Statements;

namespace Lending.Part2.Archive;

public sealed record PaymentReversalRecord(string LoanId, string PaymentId, decimal Amount, string ReasonCode, DateTimeOffset ReversedAt);

/// <summary>
/// Writes servicing records into the archive and loads them back for audits. A record is
/// only rehydrated after its HMAC seal verifies and only when its stored type is one of
/// the archivable record types listed here.
/// </summary>
public sealed class ArchiveReplayService(ArchiveBlobStore blobs, ArchiveSealer sealer, ArchivePayloadSerializer serializer)
{
    private static readonly HashSet<string> ArchivableTypes = new(StringComparer.Ordinal)
    {
        typeof(MonthlyStatement).AssemblyQualifiedName!,
        typeof(PaymentReversalRecord).AssemblyQualifiedName!,
    };

    public Task<string> ArchiveAsync(object record, CancellationToken ct)
    {
        var (typeName, payload) = serializer.Serialize(record);
        if (!ArchivableTypes.Contains(typeName))
        {
            throw new ArgumentException($"{record.GetType().Name} is not an archivable record type.", nameof(record));
        }
        return blobs.WriteAsync(sealer.Seal(typeName, payload), ct);
    }

    public async Task<object?> LoadAsync(string blobName, CancellationToken ct)
    {
        var sealedRecord = await blobs.ReadAsync(blobName, ct);
        if (sealedRecord is null || !sealer.Verify(sealedRecord) || !ArchivableTypes.Contains(sealedRecord.TypeName))
        {
            return null;
        }
        return serializer.Deserialize(sealedRecord.TypeName, sealedRecord.Payload);
    }
}
