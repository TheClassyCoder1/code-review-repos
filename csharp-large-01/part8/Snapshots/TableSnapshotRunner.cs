using System.Diagnostics;

namespace Lending.Part8.Snapshots;

public sealed class SnapshotOptions
{
    public string OutputDir { get; set; } = "/var/lib/servicing/snapshots";
    public string Database { get; set; } = "lending";
}

public sealed class TableSnapshotRunner(SnapshotOptions options, ILogger<TableSnapshotRunner> logger)
{
    public async Task<string> DumpTableAsync(string tableName, CancellationToken ct)
    {
        var outputFile = $"{options.OutputDir}/{tableName}-{DateTime.UtcNow:yyyyMMddHHmmss}.sql.gz";
        var startInfo = new ProcessStartInfo("/bin/sh")
        {
            Arguments = $"-c \"pg_dump --data-only --table={tableName} {options.Database} | gzip > {outputFile}\"",
            RedirectStandardError = true,
            UseShellExecute = false,
        };

        using var process = Process.Start(startInfo)
            ?? throw new InvalidOperationException("snapshot shell could not be started");
        var stderr = await process.StandardError.ReadToEndAsync(ct);
        await process.WaitForExitAsync(ct);
        if (process.ExitCode != 0)
        {
            logger.LogError("Snapshot of {Table} failed with {ExitCode}: {Stderr}", tableName, process.ExitCode, stderr);
            throw new InvalidOperationException($"pg_dump exited with {process.ExitCode}");
        }

        logger.LogInformation("Snapshot of {Table} written to {File}", tableName, outputFile);
        return outputFile;
    }
}
