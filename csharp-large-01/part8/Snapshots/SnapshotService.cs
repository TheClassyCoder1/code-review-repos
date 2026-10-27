namespace Lending.Part8.Snapshots;

public sealed class SnapshotService(TableSnapshotRunner runner)
{
    public async Task<string?> SnapshotAsync(string requestedTable, CancellationToken ct)
    {
        if (!SnapshotTableCatalog.TryResolve(requestedTable, out var tableName))
        {
            return null;
        }

        return await runner.DumpTableAsync(tableName, ct);
    }
}
