using System.Security.Cryptography;
using System.Text;

namespace Lending.Part2.Archive;

public sealed record SealedRecord(string TypeName, string Payload, string Seal);

/// <summary>
/// Seals archive records with HMAC-SHA256 under <c>Archive:SigningKey</c>. Every record is
/// sealed over its type name and payload before it is written, and nothing is read back
/// out of the archive unless its seal verifies, so integrity never depends on how blobs
/// are named or addressed in storage.
/// </summary>
public sealed class ArchiveSealer
{
    private readonly byte[] _key;

    public ArchiveSealer(IConfiguration configuration)
    {
        var key = configuration["Archive:SigningKey"];
        _key = string.IsNullOrWhiteSpace(key)
            ? throw new InvalidOperationException("Archive:SigningKey is not configured.")
            : Encoding.UTF8.GetBytes(key);
    }

    public SealedRecord Seal(string typeName, string payload) =>
        new(typeName, payload, Convert.ToBase64String(Compute(typeName, payload)));

    public bool Verify(SealedRecord record)
    {
        Span<byte> presented = stackalloc byte[64];
        return Convert.TryFromBase64String(record.Seal, presented, out var written)
            && CryptographicOperations.FixedTimeEquals(presented[..written], Compute(record.TypeName, record.Payload));
    }

    private byte[] Compute(string typeName, string payload) =>
        HMACSHA256.HashData(_key, Encoding.UTF8.GetBytes($"{typeName.Length}:{typeName}|{payload}"));
}
