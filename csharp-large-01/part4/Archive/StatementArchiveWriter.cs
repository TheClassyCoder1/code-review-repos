using System.Globalization;

namespace Lending.Servicing.Part4.Archive;

/// <summary>
/// Lays out the on-disk statement archive: one directory per loan, one file
/// per statement, sharded by year and month so directories stay small.
/// </summary>
public sealed class StatementArchiveWriter
{
    private const int WriteBufferSize = 64 * 1024;

    private readonly string _rootDirectory;

    public StatementArchiveWriter(string rootDirectory)
    {
        if (string.IsNullOrWhiteSpace(rootDirectory))
        {
            throw new ArgumentException("Archive root directory is required.", nameof(rootDirectory));
        }

        _rootDirectory = Path.GetFullPath(rootDirectory);
    }

    public string RootDirectory => _rootDirectory;

    /// <summary>
    /// Opens a fresh archive entry for writing. The returned stream is owned by
    /// the caller, which is responsible for flushing and disposing it.
    /// </summary>
    public Stream OpenEntryStream(string loanId, string statementId, DateOnly periodEnd)
    {
        var path = ResolveEntryPath(loanId, statementId, periodEnd);
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);

        var stream = new FileStream(path, FileMode.Create, FileAccess.Write, FileShare.None, WriteBufferSize, useAsync: true);
        return stream;
    }

    public string ResolveEntryPath(string loanId, string statementId, DateOnly periodEnd)
    {
        var safeLoanId = SanitizeSegment(loanId, nameof(loanId));
        var safeStatementId = SanitizeSegment(statementId, nameof(statementId));
        var year = periodEnd.Year.ToString("D4", CultureInfo.InvariantCulture);
        var month = periodEnd.Month.ToString("D2", CultureInfo.InvariantCulture);

        var path = Path.GetFullPath(Path.Combine(_rootDirectory, year, month, safeLoanId, safeStatementId + ".csv"));
        if (!path.StartsWith(_rootDirectory + Path.DirectorySeparatorChar, StringComparison.Ordinal))
        {
            throw new InvalidOperationException("Resolved archive path escapes the archive root.");
        }

        return path;
    }

    public bool EntryExists(string loanId, string statementId, DateOnly periodEnd) =>
        File.Exists(ResolveEntryPath(loanId, statementId, periodEnd));

    private static string SanitizeSegment(string value, string parameterName)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            throw new ArgumentException("Archive path segment is required.", parameterName);
        }

        foreach (var ch in value)
        {
            if (!(char.IsLetterOrDigit(ch) || ch == '-' || ch == '_'))
            {
                throw new ArgumentException($"Archive path segment contains an unsupported character '{ch}'.", parameterName);
            }
        }

        return value;
    }
}
