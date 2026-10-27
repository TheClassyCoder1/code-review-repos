using System.Text;
using Lending.Servicing.Part4.Domain;
using Lending.Servicing.Part4.Infrastructure;

namespace Lending.Servicing.Part4.Archive;

public interface IStatementSource
{
    IAsyncEnumerable<Statement> ReadClosedStatementsAsync(DateOnly periodEnd, CancellationToken cancellationToken);
}

public sealed record StatementExportResult(int Written, int Skipped, int Failed);

/// <summary>
/// Nightly job that archives every statement closed on a given period end.
/// Already-archived statements are skipped so the job can be re-run safely.
/// </summary>
public sealed class StatementExportJob
{
    private static readonly UTF8Encoding Utf8NoBom = new(encoderShouldEmitUTF8Identifier: false);

    private readonly IStatementSource _source;
    private readonly StatementArchiveWriter _archive;
    private readonly IServicingLog _log;

    public StatementExportJob(IStatementSource source, StatementArchiveWriter archive, IServicingLog log)
    {
        _source = source;
        _archive = archive;
        _log = log;
    }

    public async Task<StatementExportResult> RunAsync(DateOnly periodEnd, CancellationToken cancellationToken)
    {
        var written = 0;
        var skipped = 0;
        var failed = 0;

        await foreach (var statement in _source.ReadClosedStatementsAsync(periodEnd, cancellationToken).ConfigureAwait(false))
        {
            if (_archive.EntryExists(statement.LoanId, statement.StatementId, statement.PeriodEnd))
            {
                skipped++;
                continue;
            }

            try
            {
                await ExportOneAsync(statement, cancellationToken).ConfigureAwait(false);
                written++;
            }
            catch (IOException ex)
            {
                failed++;
                _log.Error(ex, "Archiving statement {0} for loan {1} failed", statement.StatementId, statement.LoanId);
            }
        }

        _log.Info("Statement export for {0:yyyy-MM-dd}: written={1} skipped={2} failed={3}", periodEnd, written, skipped, failed);
        return new StatementExportResult(written, skipped, failed);
    }

    private async Task ExportOneAsync(Statement statement, CancellationToken cancellationToken)
    {
        await using var stream = _archive.OpenEntryStream(statement.LoanId, statement.StatementId, statement.PeriodEnd);
        await using var writer = new StreamWriter(stream, Utf8NoBom, bufferSize: 16 * 1024, leaveOpen: true);

        await StatementRenderer.WriteCsvAsync(statement, writer, cancellationToken).ConfigureAwait(false);
        await writer.FlushAsync(cancellationToken).ConfigureAwait(false);
        await stream.FlushAsync(cancellationToken).ConfigureAwait(false);
    }
}
