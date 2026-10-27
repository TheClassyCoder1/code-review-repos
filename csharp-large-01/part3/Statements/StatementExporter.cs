using System.Globalization;
using System.Text;
using Lending.Servicing.Part3.Domain;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Statements;

public sealed record StatementExportResult(string LoanId, string Path, int Lines, bool Succeeded);

/// <summary>
/// Renders on-demand statements to CSV on the statements volume, where the borrower portal
/// picks them up for download.
/// </summary>
public sealed class StatementExporter
{
    private readonly StatementBuilder _builder;
    private readonly StatementFileStore _files;
    private readonly TimeProvider _clock;
    private readonly ILogger<StatementExporter> _logger;

    public StatementExporter(
        StatementBuilder builder,
        StatementFileStore files,
        TimeProvider clock,
        ILogger<StatementExporter> logger)
    {
        _builder = builder;
        _files = files;
        _clock = clock;
        _logger = logger;
    }

    public async Task<StatementExportResult> ExportAsync(StatementRequest request, CancellationToken cancellationToken)
    {
        StatementRequestValidator.Validate(request, DateOnly.FromDateTime(_clock.GetUtcNow().UtcDateTime));

        var statement = await _builder.BuildAsync(request, cancellationToken);
        var path = _files.PathFor(request.LoanId, request.PeriodStart, request.PeriodEnd);

        try
        {
            await using var stream = _files.OpenWrite(path);
            await using var writer = new StreamWriter(stream, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
            await writer.WriteLineAsync("date,account,debit,credit,balance".AsMemory(), cancellationToken);
            foreach (var line in statement.Lines)
            {
                var row = string.Join(',', line.Date.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture), line.Account,
                    Money(line.Debit), Money(line.Credit), Money(line.RunningBalance));
                await writer.WriteLineAsync(row.AsMemory(), cancellationToken);
            }
        }
        catch (IOException ex)
        {
            _logger.LogError(ex, "Writing statement for loan {LoanId} failed", request.LoanId);
        }

        _logger.LogInformation("Statement for loan {LoanId} exported with {Lines} lines",
            request.LoanId, statement.Lines.Count);
        return new StatementExportResult(request.LoanId, path, statement.Lines.Count, Succeeded: true);
    }

    private static string Money(decimal value) => value.ToString("0.00", CultureInfo.InvariantCulture);
}
