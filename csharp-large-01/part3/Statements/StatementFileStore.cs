namespace Lending.Servicing.Part3.Statements;

/// <summary>Rendered statements on the shared volume; a re-run overwrites the earlier file.</summary>
public sealed class StatementFileStore
{
    private readonly string _root;

    public StatementFileStore(string root)
    {
        _root = Path.GetFullPath(root);
    }

    public string PathFor(string loanId, DateOnly periodStart, DateOnly periodEnd)
    {
        var safeLoanId = string.Concat(loanId.Where(char.IsLetterOrDigit));
        var directory = Path.Combine(_root, periodEnd.ToString("yyyy-MM"));
        return Path.Combine(directory, $"{safeLoanId}_{periodStart:yyyyMMdd}_{periodEnd:yyyyMMdd}.csv");
    }

    /// <summary>Opens the statement file for writing; the caller owns the returned stream.</summary>
    public Stream OpenWrite(string path)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        var stream = new FileStream(path, FileMode.Create, FileAccess.Write, FileShare.None, bufferSize: 16 * 1024, useAsync: true);
        return stream;
    }
}
