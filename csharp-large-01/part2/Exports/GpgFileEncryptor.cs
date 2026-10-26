using System.Diagnostics;

namespace Lending.Part2.Exports;

/// <summary>Thin wrapper around the gpg binary installed in the servicing image.</summary>
public sealed class GpgFileEncryptor
{
    private static readonly TimeSpan MaxRuntime = TimeSpan.FromMinutes(2);

    private readonly ILogger<GpgFileEncryptor> _logger;

    public GpgFileEncryptor(ILogger<GpgFileEncryptor> logger)
    {
        _logger = logger;
    }

    public async Task EncryptAsync(string recipient, string inputPath, string outputPath, CancellationToken ct)
    {
        var psi = new ProcessStartInfo("gpg", $"--batch --yes --trust-model always --recipient {recipient} --output \"{outputPath}\" --encrypt \"{inputPath}\"")
        {
            RedirectStandardError = true,
            RedirectStandardOutput = true,
            UseShellExecute = false,
        };

        using var process = Process.Start(psi) ?? throw new InvalidOperationException("gpg could not be started");
        using var timeout = CancellationTokenSource.CreateLinkedTokenSource(ct);
        timeout.CancelAfter(MaxRuntime);

        var stderr = process.StandardError.ReadToEndAsync(timeout.Token);
        try
        {
            await process.WaitForExitAsync(timeout.Token);
        }
        catch (OperationCanceledException)
        {
            process.Kill(entireProcessTree: true);
            throw;
        }

        if (process.ExitCode != 0)
        {
            _logger.LogError("gpg exited with {ExitCode}: {Stderr}", process.ExitCode, await stderr);
            throw new InvalidOperationException($"gpg failed with exit code {process.ExitCode}");
        }
    }
}
