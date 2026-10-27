namespace Lending.Part8.Statements;

public sealed class StatementPdfStore(IConfiguration configuration)
{
    private readonly string _root = Path.GetFullPath(configuration["Statements:PdfRoot"] ?? "/var/lib/servicing/statements");

    public Stream? OpenRead(string loanId, string period)
    {
        if (!Guid.TryParse(loanId, out var loanGuid) || !DateOnly.TryParseExact(period, "yyyy-MM", out var month))
        {
            return null;
        }

        var path = Path.Combine(_root, loanGuid.ToString("N"), $"{month:yyyy-MM}.pdf");
        if (!File.Exists(path))
        {
            return null;
        }

        var stream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read, 81920, useAsync: true);
        return stream;
    }
}
