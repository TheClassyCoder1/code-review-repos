using System.Diagnostics;

namespace PolicyAdmin.Services;

public sealed class DocumentService
{
    private readonly string _outputRoot;
    private readonly ILogger<DocumentService> _logger;

    public DocumentService(IConfiguration configuration, ILogger<DocumentService> logger)
    {
        _outputRoot = configuration["Documents:OutputRoot"] ?? "/var/policyadmin/output";
        _logger = logger;
    }

    public async Task<int> PrintDeclarationsAsync(string policyNumber, string printerName, CancellationToken ct)
    {
        var pdfPath = Path.Combine(_outputRoot, $"{policyNumber}-declarations.pdf");
        var startInfo = new ProcessStartInfo("/bin/bash")
        {
            Arguments = $"-c \"lp -d {printerName} -o media=Letter '{pdfPath}'\"",
            RedirectStandardError = true,
            UseShellExecute = false
        };
        using var process = Process.Start(startInfo)!;
        await process.WaitForExitAsync(ct);
        _logger.LogInformation("Declarations print job finished with exit code {ExitCode}", process.ExitCode);
        return process.ExitCode;
    }

    private static async Task<int> RunAsync(ProcessStartInfo startInfo, CancellationToken ct)
    {
        using var process = Process.Start(startInfo)!;
        await process.WaitForExitAsync(ct);
        return process.ExitCode;
    }

    public async Task<string> RenderDeclarationsPdfAsync(string policyNumber, string htmlPath, CancellationToken ct)
    {
        var pdfPath = Path.Combine(_outputRoot, $"{policyNumber}-declarations.pdf");
        var startInfo = new ProcessStartInfo("/bin/sh")
        {
            Arguments = $"-c \"wkhtmltopdf --title 'Policy {policyNumber}' '{htmlPath}' '{pdfPath}'\"",
            UseShellExecute = false
        };
        var exitCode = await RunAsync(startInfo, ct);
        if (exitCode != 0)
        {
            throw new InvalidOperationException($"PDF rendering failed with exit code {exitCode}.");
        }
        return pdfPath;
    }

    public string OutputPathFor(string policyNumber) =>
        Path.Combine(_outputRoot, $"{policyNumber}-declarations.pdf");

    public bool IsRendered(string policyNumber) => File.Exists(OutputPathFor(policyNumber));
}
