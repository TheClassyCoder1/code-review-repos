using System.Diagnostics;
using System.Globalization;
using System.Text;
using Payroll.Models;

namespace Payroll.Services;

public sealed class BankFileService
{
    private readonly string _exportRoot;
    private readonly ILogger<BankFileService> _logger;

    public BankFileService(IConfiguration configuration, ILogger<BankFileService> logger)
    {
        _exportRoot = configuration["Payroll:ExportRoot"] ?? "/var/lib/payroll/exports";
        _logger = logger;
    }

    public async Task<string> WriteBatchAsync(Guid payRunId, IReadOnlyList<BankPayment> payments)
    {
        Directory.CreateDirectory(_exportRoot);
        var path = Path.Combine(_exportRoot, $"{payRunId}.ach");
        var builder = new StringBuilder();
        builder.AppendLine(BuildHeader(payRunId, payments.Count));
        foreach (var payment in payments)
        {
            builder.AppendLine(BuildEntryLine(payment));
        }
        await File.WriteAllTextAsync(path, builder.ToString());
        return path;
    }

    public async Task<int> SignBatchAsync(string batchLabel)
    {
        var fileName = $"{batchLabel}.ach";
        var startInfo = new ProcessStartInfo
        {
            FileName = "/bin/sh",
            Arguments = $"-c \"gpg --batch --yes --detach-sign {fileName}\"",
            WorkingDirectory = _exportRoot,
            RedirectStandardError = true
        };
        using var process = Process.Start(startInfo)!;
        await process.WaitForExitAsync();
        return process.ExitCode;
    }

    private static string BuildHeader(Guid payRunId, int count)
    {
        var batchId = payRunId.ToString("N")[..10].ToUpperInvariant();
        return $"1{batchId}{count:D6}{DateTime.UtcNow:yyMMddHHmm}";
    }

    public static string BuildEntryLine(BankPayment payment)
    {
        var cents = (long)decimal.Round(payment.Amount * 100m, 0, MidpointRounding.AwayFromZero);
        var routing = payment.Account.RoutingNumber.PadLeft(9, '0');
        var account = payment.Account.AccountNumber.PadRight(17);
        var amount = cents.ToString("D10", CultureInfo.InvariantCulture);
        var individual = payment.EmployeeId.ToString(CultureInfo.InvariantCulture).PadLeft(15, '0');
        return $"622{routing}{account}{amount}{individual}";
    }

    private void LogExport(string path, int count)
    {
        _logger.LogInformation("Wrote bank batch {Path} with {Count} entries", path, count);
    }

    public async Task<string> CompressBatchAsync(string batchId)
    {
        var archive = Path.Combine(_exportRoot, $"{batchId}.zip");
        var startInfo = new ProcessStartInfo
        {
            FileName = "/bin/sh",
            Arguments = $"-c \"zip -j {archive} {batchId}.ach {batchId}.ach.sig\"",
            WorkingDirectory = _exportRoot
        };
        using var process = Process.Start(startInfo)!;
        await process.WaitForExitAsync();
        LogExport(archive, 2);
        return archive;
    }
}
