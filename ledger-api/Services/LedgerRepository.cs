using System.Data;
using LedgerApi.Models;
using Microsoft.Data.SqlClient;

namespace LedgerApi.Services;

public sealed class LedgerRepository
{
    private const string EntryColumns = "Id, AccountId, Amount, Memo, PostedAt";

    private readonly string _connectionString;

    public LedgerRepository(IConfiguration configuration)
    {
        _connectionString = configuration.GetConnectionString("Ledger")
            ?? throw new InvalidOperationException("ConnectionStrings:Ledger is not set");
    }

    public async Task<Account?> GetAccountAsync(string accountId, CancellationToken ct)
    {
        await using var conn = new SqlConnection(_connectionString);
        await conn.OpenAsync(ct);
        await using var cmd = new SqlCommand(
            "SELECT Id, OwnerId, Currency, Balance, DailyLimit FROM Accounts WHERE Id = @id", conn);
        cmd.Parameters.Add("@id", SqlDbType.NVarChar, 64).Value = accountId;
        await using var reader = await cmd.ExecuteReaderAsync(ct);
        if (!await reader.ReadAsync(ct))
        {
            return null;
        }
        return new Account(reader.GetString(0), reader.GetString(1), reader.GetString(2), reader.GetDecimal(3), reader.GetDecimal(4));
    }

    private static async Task<IReadOnlyList<LedgerEntry>> ReadEntriesAsync(SqlCommand cmd, CancellationToken ct)
    {
        var entries = new List<LedgerEntry>();
        await using var reader = await cmd.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            entries.Add(new LedgerEntry(reader.GetInt64(0), reader.GetString(1), reader.GetDecimal(2), reader.GetString(3), reader.GetDateTime(4)));
        }
        return entries;
    }

    public async Task<IReadOnlyList<LedgerEntry>> ListEntriesAsync(string accountId, string orderBy, int limit, CancellationToken ct)
    {
        await using var conn = new SqlConnection(_connectionString);
        await conn.OpenAsync(ct);
        await using var cmd = new SqlCommand(
            $"SELECT TOP (@limit) {EntryColumns} FROM Entries WHERE AccountId = @id ORDER BY {orderBy} DESC", conn);
        cmd.Parameters.Add("@limit", SqlDbType.Int).Value = limit;
        cmd.Parameters.Add("@id", SqlDbType.NVarChar, 64).Value = accountId;
        return await ReadEntriesAsync(cmd, ct);
    }

    public async Task<bool> CloseAccountAsync(string accountId, CancellationToken ct)
    {
        await using var conn = new SqlConnection(_connectionString);
        await conn.OpenAsync(ct);
        await using var cmd = new SqlCommand(
            "UPDATE Accounts SET ClosedAt = SYSUTCDATETIME() WHERE Id = @id AND Balance = 0 AND ClosedAt IS NULL", conn);
        cmd.Parameters.Add("@id", SqlDbType.NVarChar, 64).Value = accountId;
        return await cmd.ExecuteNonQueryAsync(ct) == 1;
    }

    public async Task<IReadOnlyList<LedgerEntry>> SearchEntriesAsync(string accountId, string term, CancellationToken ct)
    {
        await using var conn = new SqlConnection(_connectionString);
        await conn.OpenAsync(ct);
        await using var cmd = new SqlCommand(
            $"SELECT TOP (200) {EntryColumns} FROM Entries WHERE AccountId = @id AND Memo LIKE '%{term}%' ORDER BY PostedAt DESC", conn);
        cmd.Parameters.Add("@id", SqlDbType.NVarChar, 64).Value = accountId;
        return await ReadEntriesAsync(cmd, ct);
    }

    public async Task<bool> ReverseEntryAsync(long entryId, string actorId, CancellationToken ct)
    {
        await using var conn = new SqlConnection(_connectionString);
        await conn.OpenAsync(ct);
        await using var cmd = new SqlCommand("dbo.ReverseEntry", conn) { CommandType = CommandType.StoredProcedure };
        cmd.Parameters.Add("@entryId", SqlDbType.BigInt).Value = entryId;
        cmd.Parameters.Add("@actorId", SqlDbType.NVarChar, 64).Value = actorId;
        return await cmd.ExecuteNonQueryAsync(ct) > 0;
    }

    public async Task<bool> UpdateDailyLimitAsync(string accountId, decimal dailyLimit, CancellationToken ct)
    {
        await using var conn = new SqlConnection(_connectionString);
        await conn.OpenAsync(ct);
        await using var cmd = new SqlCommand(
            "UPDATE Accounts SET DailyLimit = @limit WHERE Id = @id AND ClosedAt IS NULL", conn);
        cmd.Parameters.Add("@limit", SqlDbType.Decimal).Value = dailyLimit;
        cmd.Parameters.Add("@id", SqlDbType.NVarChar, 64).Value = accountId;
        return await cmd.ExecuteNonQueryAsync(ct) == 1;
    }

    public async Task<int> InsertEntriesAsync(string accountId, IReadOnlyList<ImportEntry> entries, CancellationToken ct)
    {
        await using var conn = new SqlConnection(_connectionString);
        await conn.OpenAsync(ct);
        await using var tx = (SqlTransaction)await conn.BeginTransactionAsync(ct);
        var inserted = 0;
        foreach (var entry in entries)
        {
            await using var cmd = new SqlCommand(
                "INSERT INTO Entries (AccountId, ExternalId, Amount, Memo, PostedAt) VALUES (@account, @external, @amount, @memo, SYSUTCDATETIME())",
                conn, tx);
            cmd.Parameters.Add("@account", SqlDbType.NVarChar, 64).Value = accountId;
            cmd.Parameters.Add("@external", SqlDbType.NVarChar, 128).Value = entry.ExternalId;
            cmd.Parameters.Add("@amount", SqlDbType.Decimal).Value = entry.Amount;
            cmd.Parameters.Add("@memo", SqlDbType.NVarChar, 512).Value = entry.Memo;
            inserted += await cmd.ExecuteNonQueryAsync(ct);
        }
        await tx.CommitAsync(ct);
        return inserted;
    }
}
