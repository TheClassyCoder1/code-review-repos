namespace Lending.Part8.Intake;

public sealed record StagedDocument(Guid Id, string Path, long Length);

/// <summary>
/// Writes uploaded documents into the staging directory under a server-generated name.
/// The client-supplied file name is never part of the stored path.
/// </summary>
public sealed class StagedDocumentStore(IConfiguration configuration)
{
    private readonly string _stagingDir = System.IO.Path.GetFullPath(
        configuration["Intake:StagingDir"] ?? "/var/lib/servicing/intake");

    public async Task<StagedDocument> SaveAsync(Stream content, CancellationToken ct)
    {
        Directory.CreateDirectory(_stagingDir);
        var id = Guid.NewGuid();
        var path = System.IO.Path.Combine(_stagingDir, $"{id:N}.pdf");

        await using var target = new FileStream(path, FileMode.CreateNew, FileAccess.Write, FileShare.None);
        await content.CopyToAsync(target, ct);
        return new StagedDocument(id, path, target.Length);
    }
}
