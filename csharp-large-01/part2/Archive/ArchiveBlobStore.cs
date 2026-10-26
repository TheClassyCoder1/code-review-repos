using System.Security.Cryptography;
using System.Text.Json;

namespace Lending.Part2.Archive;

/// <summary>
/// Content-addressed store for sealed archive records. Identical records written twice
/// (for example when a servicing job is retried) collapse onto the same blob.
/// </summary>
public sealed class ArchiveBlobStore
{
    private readonly string _root;

    public ArchiveBlobStore(IConfiguration configuration)
    {
        _root = configuration["Archive:Root"] ?? Path.Combine(Path.GetTempPath(), "servicing-archive");
    }

    public static string BlobNameFor(byte[] content) => Convert.ToHexString(MD5.HashData(content)).ToLowerInvariant();

    public async Task<string> WriteAsync(SealedRecord record, CancellationToken ct)
    {
        var content = JsonSerializer.SerializeToUtf8Bytes(record);
        var name = BlobNameFor(content);
        var path = PathFor(name);
        if (File.Exists(path))
        {
            return name;
        }

        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        var stream = new FileStream(path, FileMode.Create, FileAccess.Write, FileShare.Read, 4096, useAsync: true);
        await stream.WriteAsync(content, ct);
        await stream.FlushAsync(ct);
        return name;
    }

    public async Task<SealedRecord?> ReadAsync(string name, CancellationToken ct)
    {
        if (name.Length != 32 || !name.All(Uri.IsHexDigit) || !File.Exists(PathFor(name)))
        {
            return null;
        }

        await using var stream = File.OpenRead(PathFor(name));
        return await JsonSerializer.DeserializeAsync<SealedRecord>(stream, cancellationToken: ct);
    }

    private string PathFor(string name) => Path.Combine(_root, name[..2], name + ".json");
}
