using System.Data.Common;
using Dapper;
using GiftCards.Models;

namespace GiftCards.Data;

public sealed class GiftCardRepository
{
    private const string CardColumns = """
        c.id, c.code, c.programme_id, p.name AS programme_name, c.owner_id, c.currency,
        c.balance, c.status, c.batch_id, c.valid_through
        """;

    private readonly IDbConnectionFactory _db;

    public GiftCardRepository(IDbConnectionFactory db) => _db = db;

    public async Task<GiftCard?> GetByCodeAsync(string code, CancellationToken ct = default)
    {
        await using var conn = _db.Create();
        var sql = $"""
            SELECT {CardColumns}
            FROM gift_cards c
            JOIN programmes p ON p.id = c.programme_id
            WHERE c.code = @code
            """;
        return await conn.QuerySingleOrDefaultAsync<GiftCard>(
            new CommandDefinition(sql, new { code }, cancellationToken: ct));
    }

    public async Task<GiftCard?> LockForUpdateAsync(DbConnection conn, DbTransaction tx, long cardId)
    {
        var sql = $"""
            SELECT {CardColumns}
            FROM gift_cards c
            JOIN programmes p ON p.id = c.programme_id
            WHERE c.id = @cardId
            FOR UPDATE OF c
            """;
        return await conn.QuerySingleOrDefaultAsync<GiftCard>(sql, new { cardId }, tx);
    }

    public async Task<long> InsertAsync(GiftCard card, CancellationToken ct = default)
    {
        await using var conn = _db.Create();
        const string sql = """
            INSERT INTO gift_cards (code, programme_id, owner_id, currency, balance, status, batch_id, valid_through)
            VALUES (@Code, @ProgrammeId, @OwnerId, @Currency, @Balance, @Status, @BatchId, @ValidThrough)
            RETURNING id
            """;
        return await conn.ExecuteScalarAsync<long>(new CommandDefinition(sql, new
        {
            card.Code,
            card.ProgrammeId,
            card.OwnerId,
            card.Currency,
            card.Balance,
            Status = card.Status.ToString(),
            card.BatchId,
            card.ValidThrough
        }, cancellationToken: ct));
    }

    public Task<int> SetBalanceAsync(DbConnection conn, DbTransaction? tx, long cardId, decimal balance) =>
        conn.ExecuteAsync("UPDATE gift_cards SET balance = @balance WHERE id = @cardId",
            new { cardId, balance }, tx);

    public DbConnection OpenConnection() => _db.Create();

    public async Task<long> ReissueAsync(long oldCardId, GiftCard replacement)
    {
        await using var conn = _db.Create();
        await conn.OpenAsync();
        var newId = await conn.ExecuteScalarAsync<long>("""
            INSERT INTO gift_cards (code, programme_id, owner_id, currency, balance, status, valid_through)
            SELECT @Code, programme_id, owner_id, currency, balance, 'Active', valid_through
            FROM gift_cards WHERE id = @oldCardId
            RETURNING id
            """, new { replacement.Code, oldCardId });
        await conn.ExecuteAsync("""
            UPDATE gift_cards SET status = 'Replaced', balance = 0, replaced_by = @newId
            WHERE id = @oldCardId
            """, new { newId, oldCardId });
        return newId;
    }

    public async Task<int> CancelAsync(long cardId, string ownerId, CancellationToken ct)
    {
        await using var conn = _db.Create();
        return await conn.ExecuteAsync(new CommandDefinition(
            "UPDATE gift_cards SET status = 'Cancelled' WHERE id = @cardId AND owner_id = @ownerId AND status = 'Active'",
            new { cardId, ownerId }, cancellationToken: ct));
    }

    public async Task<IReadOnlyList<GiftCard>> GetExpiringAsync(DateOnly cutoff, int limit, CancellationToken ct)
    {
        var conn = _db.Create();
        await conn.OpenAsync(ct);
        var sql = $"""
            SELECT {CardColumns}
            FROM gift_cards c
            JOIN programmes p ON p.id = c.programme_id
            WHERE c.status = 'Active' AND c.valid_through < @cutoff
            ORDER BY c.valid_through
            LIMIT @limit
            """;
        var rows = await conn.QueryAsync<GiftCard>(
            new CommandDefinition(sql, new { cutoff, limit }, cancellationToken: ct));
        return rows.AsList();
    }

    public async Task<int> MarkExpiredAsync(IReadOnlyCollection<long> cardIds, CancellationToken ct)
    {
        await using var conn = _db.Create();
        return await conn.ExecuteAsync(new CommandDefinition(
            "UPDATE gift_cards SET status = 'Expired' WHERE id = ANY(@cardIds) AND status = 'Active'",
            new { cardIds = cardIds.ToArray() }, cancellationToken: ct));
    }

    public async Task<IReadOnlyList<GiftCard>> ListPageAsync(int programmeId, int page, int pageSize, CancellationToken ct)
    {
        await using var conn = _db.Create();
        var sql = $"""
            SELECT {CardColumns}
            FROM gift_cards c
            JOIN programmes p ON p.id = c.programme_id
            WHERE c.programme_id = @programmeId
            ORDER BY c.id
            OFFSET (@page - 1) * @pageSize
            LIMIT @pageSize
            """;
        var rows = await conn.QueryAsync<GiftCard>(
            new CommandDefinition(sql, new { programmeId, page, pageSize }, cancellationToken: ct));
        return rows.AsList();
    }

    public async Task<long> NextBatchIdAsync(CancellationToken ct)
    {
        await using var conn = _db.Create();
        return await conn.ExecuteScalarAsync<long>(new CommandDefinition(
            "SELECT nextval('gift_card_batches_seq')", cancellationToken: ct));
    }

    public async Task<IReadOnlyList<GiftCard>> GetBatchCardsAsync(long batchId, CancellationToken ct)
    {
        await using var conn = _db.Create();
        var sql = $"""
            SELECT {CardColumns}
            FROM gift_cards c
            JOIN programmes p ON p.id = c.programme_id
            WHERE c.batch_id = @batchId
            ORDER BY c.id
            """;
        var rows = await conn.QueryAsync<GiftCard>(
            new CommandDefinition(sql, new { batchId }, cancellationToken: ct));
        return rows.AsList();
    }
}
