using System.Diagnostics;

namespace Lending.Part8.Exports;

public sealed class GpgExportEncryptor(ILogger<GpgExportEncryptor> logger)
{
    public async Task<string> EncryptAsync(string inputPath, string recipientFingerprint, CancellationToken ct)
    {
        var outputPath = inputPath + ".gpg";
        var startInfo = new ProcessStartInfo("gpg")
        {
            Arguments = $"--batch --yes --trust-model always -r {recipientFingerprint} -o \"{outputPath}\" -e \"{inputPath}\"",
            RedirectStandardError = true,
            UseShellExecute = false,
        };

        using var process = Process.Start(startInfo)
            ?? throw new InvalidOperationException("gpg could not be started");
        var stderr = await process.StandardError.ReadToEndAsync(ct);
        await process.WaitForExitAsync(ct);
        if (process.ExitCode != 0)
        {
            logger.LogError("gpg exited with {ExitCode}: {Stderr}", process.ExitCode, stderr);
            throw new InvalidOperationException($"gpg exited with {process.ExitCode}");
        }

        return outputPath;
    }
}
