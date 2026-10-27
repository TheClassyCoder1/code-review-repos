namespace Lending.Part8.Exports;

public sealed class PartnerFileExporter(
    PartnerKeyRegistry keys,
    GpgExportEncryptor encryptor,
    IConfiguration configuration)
{
    public async Task<string> ExportAsync(string partnerCode, IEnumerable<string> csvLines, CancellationToken ct)
    {
        var fingerprint = keys.ResolveFingerprint(partnerCode);

        var exportDir = configuration["Exports:WorkDir"] ?? Path.GetTempPath();
        var plainPath = Path.Combine(exportDir, $"export-{Guid.NewGuid():N}.csv");
        await File.WriteAllLinesAsync(plainPath, csvLines, ct);
        try
        {
            return await encryptor.EncryptAsync(plainPath, fingerprint, ct);
        }
        finally
        {
            File.Delete(plainPath);
        }
    }
}
