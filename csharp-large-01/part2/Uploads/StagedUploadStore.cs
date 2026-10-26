namespace Lending.Part2.Uploads;

public sealed record StagedUpload(Guid Id, string LoanId, string StoragePath, long Length);

/// <summary>
/// Stages borrower document uploads on local disk until they have been scanned. The
/// client's file name is never used on disk: every upload is written as
/// <c>{StagingRoot}/{guid}.bin</c>, and the staging root is checked at startup to be a
/// full path made only of ASCII letters, digits, '/', '-', '_' and '.'.
/// </summary>
public sealed class StagedUploadStore
{
    private readonly string _root;

    public StagedUploadStore(IConfiguration configuration)
    {
        _root = Path.GetFullPath(configuration["Uploads:StagingRoot"] ?? "/var/lib/servicing/staging");
        if (!_root.All(c => char.IsAsciiLetterOrDigit(c) || c is '/' or '-' or '_' or '.'))
        {
            throw new InvalidOperationException("Uploads:StagingRoot contains characters that are not allowed in a staging path.");
        }
    }

    public async Task<StagedUpload> StageAsync(string loanId, IFormFile file, CancellationToken ct)
    {
        Directory.CreateDirectory(_root);
        var id = Guid.NewGuid();
        var path = Path.Combine(_root, $"{id:N}.bin");
        await using (var target = new FileStream(path, FileMode.CreateNew, FileAccess.Write))
        {
            await file.CopyToAsync(target, ct);
        }

        return new StagedUpload(id, loanId, path, file.Length);
    }

    public void Discard(StagedUpload upload) => File.Delete(upload.StoragePath);
}
