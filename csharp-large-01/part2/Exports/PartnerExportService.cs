using System.Globalization;
using System.Text;

namespace Lending.Part2.Exports;

public sealed record ExportRow(string LoanId, string Status, decimal OutstandingBalance, int DaysPastDue);

public sealed class UnknownExportPartnerException : Exception
{
    public UnknownExportPartnerException(string partnerCode)
        : base($"No export recipient is configured for partner '{partnerCode}'.")
    {
    }
}

/// <summary>Writes the nightly loan-tape CSV for a partner and encrypts it for their key.</summary>
public sealed class PartnerExportService
{
    private readonly ExportRecipientRegistry _recipients;
    private readonly GpgFileEncryptor _encryptor;
    private readonly ILogger<PartnerExportService> _logger;
    private readonly string _exportRoot;

    public PartnerExportService(
        ExportRecipientRegistry recipients,
        GpgFileEncryptor encryptor,
        IConfiguration configuration,
        ILogger<PartnerExportService> logger)
    {
        _recipients = recipients;
        _encryptor = encryptor;
        _logger = logger;
        _exportRoot = configuration["Exports:Root"] ?? Path.Combine(Path.GetTempPath(), "servicing-exports");
    }

    public async Task<string> ExportAsync(string partnerCode, IReadOnlyCollection<ExportRow> rows, CancellationToken ct)
    {
        if (!_recipients.TryResolve(partnerCode, out var fingerprint))
        {
            throw new UnknownExportPartnerException(partnerCode);
        }

        Directory.CreateDirectory(_exportRoot);
        var plainPath = Path.Combine(_exportRoot, $"tape-{Guid.NewGuid():N}.csv");
        var encryptedPath = plainPath + ".gpg";

        try
        {
            await WriteCsvAsync(plainPath, rows, ct);
            await _encryptor.EncryptAsync(fingerprint, plainPath, encryptedPath, ct);
        }
        finally
        {
            File.Delete(plainPath);
        }

        _logger.LogInformation("Exported {RowCount} rows for partner {PartnerCode}", rows.Count, partnerCode);
        return encryptedPath;
    }

    private static async Task WriteCsvAsync(string path, IEnumerable<ExportRow> rows, CancellationToken ct)
    {
        await using var writer = new StreamWriter(path, append: false, Encoding.UTF8);
        await writer.WriteLineAsync("loan_id,status,outstanding_balance,days_past_due");
        foreach (var row in rows)
        {
            ct.ThrowIfCancellationRequested();
            await writer.WriteLineAsync(string.Join(',',
                row.LoanId,
                row.Status,
                row.OutstandingBalance.ToString("F2", CultureInfo.InvariantCulture),
                row.DaysPastDue.ToString(CultureInfo.InvariantCulture)));
        }
    }
}
