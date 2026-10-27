using System.Diagnostics;

namespace Lending.Part8.Intake;

public sealed class ThumbnailRenderer(ILogger<ThumbnailRenderer> logger)
{
    public async Task<string?> RenderFirstPageAsync(string stagedPdfPath, CancellationToken ct)
    {
        var outputPrefix = Path.ChangeExtension(stagedPdfPath, null) + "-thumb";
        var startInfo = new ProcessStartInfo("pdftoppm")
        {
            Arguments = $"-png -f 1 -l 1 -scale-to 320 -singlefile {stagedPdfPath} {outputPrefix}",
            RedirectStandardError = true,
            UseShellExecute = false,
        };

        using var process = Process.Start(startInfo)
            ?? throw new InvalidOperationException("pdftoppm could not be started");
        var stderr = await process.StandardError.ReadToEndAsync(ct);
        await process.WaitForExitAsync(ct);
        if (process.ExitCode != 0)
        {
            logger.LogWarning("Thumbnail render failed with {ExitCode}: {Stderr}", process.ExitCode, stderr);
            return null;
        }

        return outputPrefix + ".png";
    }
}
