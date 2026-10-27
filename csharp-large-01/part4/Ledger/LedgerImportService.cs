using Lending.Servicing.Part4.Domain;
using Lending.Servicing.Part4.Infrastructure;

namespace Lending.Servicing.Part4.Ledger;

public interface ILedgerStore
{
    Task<int> UpsertAsync(IReadOnlyList<LedgerEntry> entries, CancellationToken cancellationToken);
}

public sealed record LedgerImportResult(string FileName, int Batches, int Entries);

/// <summary>
/// Imports a nightly ledger extract from the drop directory. The import owns
/// the file handle for the whole run; the batch reader only borrows it.
/// </summary>
public sealed class LedgerImportService
{
    private readonly LedgerCsvReader _reader;
    private readonly ILedgerStore _store;
    private readonly IServicingLog _log;

    public LedgerImportService(LedgerCsvReader reader, ILedgerStore store, IServicingLog log)
    {
        _reader = reader;
        _store = store;
        _log = log;
    }

    public async Task<LedgerImportResult> ImportAsync(string extractPath, CancellationToken cancellationToken)
    {
        var fileName = Path.GetFileName(extractPath);
        var batches = 0;
        var entries = 0;

        await using (var file = new FileStream(extractPath, FileMode.Open, FileAccess.Read, FileShare.Read, 64 * 1024, useAsync: true))
        {
            await foreach (var batch in _reader.ReadBatchesAsync(file, cancellationToken).ConfigureAwait(false))
            {
                entries += await _store.UpsertAsync(batch, cancellationToken).ConfigureAwait(false);
                batches++;
            }
        }

        _log.Info("Imported ledger extract {0}: batches={1} entries={2}", fileName, batches, entries);
        return new LedgerImportResult(fileName, batches, entries);
    }
}
