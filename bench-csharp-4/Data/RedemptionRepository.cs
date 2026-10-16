using System.Data.Common;
using Dapper;
using GiftCards.Models;
using Npgsql;

namespace GiftCards.Data;

public sealed class RedemptionRepository
{
    private readonly IDbConnectionFactory _db;

    public RedemptionRepository(IDbConnectionFactory db) => _db = db;

    public Task<long> InsertAsync(DbConnection conn, DbTransaction? tx, Redemption r) =>
        conn.ExecuteScalarAsync<long>("""
            INSERT INTO redemptions (card_id, voucher_code, merchant_id, store_id, amount, currency, receipt_no, status, processor_ref)
            VALUES (@CardId, @VoucherCode, @MerchantId, @StoreId, @Amount, @Currency, @ReceiptNo, @Status, @ProcessorRef)
            RETURNING id
            """, r, tx);

    public async Task<Redemption?> GetByReceiptAsync(string receiptNo, CancellationToken ct)
    {
        await using var conn = _db.Create();
        return await conn.QuerySingleOrDefaultAsync<Redemption>(new CommandDefinition(
            "SELECT * FROM redemptions WHERE receipt_no = @receiptNo", new { receiptNo }, cancellationToken: ct));
    }

    public async Task<bool> TryRedeemVoucherAsync(Voucher voucher, string merchantId, string receiptNo, CancellationToken ct)
    {
        await using var conn = _db.Create();
        await conn.OpenAsync(ct);

        var alreadyUsed = await conn.ExecuteScalarAsync<bool>(new CommandDefinition(
            "SELECT EXISTS (SELECT 1 FROM redemptions WHERE voucher_code = @Code AND status = 'Completed')",
            new { voucher.Code }, cancellationToken: ct));
        if (alreadyUsed)
        {
            return false;
        }

        await InsertAsync(conn, null, new Redemption
        {
            VoucherCode = voucher.Code,
            MerchantId = merchantId,
            Amount = voucher.FaceValue,
            ReceiptNo = receiptNo
        });
        return true;
    }

    public async Task<Voucher?> GetVoucherAsync(string code, CancellationToken ct)
    {
        await using var conn = _db.Create();
        return await conn.QuerySingleOrDefaultAsync<Voucher>(new CommandDefinition(
            "SELECT code AS Code, face_value AS FaceValue, valid_through AS ValidThrough FROM vouchers WHERE code = @code", new { code }, cancellationToken: ct));
    }

    public async Task<bool> ActivateAsync(long cardId, string activatedBy, CancellationToken ct)
    {
        await using var conn = _db.Create();
        await conn.OpenAsync(ct);

        var activated = await conn.ExecuteScalarAsync<bool>(new CommandDefinition(
            "SELECT EXISTS (SELECT 1 FROM card_activations WHERE card_id = @cardId)",
            new { cardId }, cancellationToken: ct));
        if (activated)
        {
            return false;
        }

        try
        {
            await conn.ExecuteAsync(new CommandDefinition(
                "INSERT INTO card_activations (card_id, activated_by) VALUES (@cardId, @activatedBy)",
                new { cardId, activatedBy }, cancellationToken: ct));
        }
        catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.UniqueViolation)
        {
            return false;
        }

        await conn.ExecuteAsync(new CommandDefinition(
            "UPDATE gift_cards SET status = 'Active' WHERE id = @cardId", new { cardId }, cancellationToken: ct));
        return true;
    }

    public async Task MarkReversedAsync(long redemptionId, CancellationToken ct)
    {
        await using var conn = _db.Create();
        await conn.ExecuteAsync(new CommandDefinition(
            "UPDATE redemptions SET status = 'Reversed' WHERE id = @redemptionId",
            new { redemptionId }, cancellationToken: ct));
    }

    public async Task<IReadOnlyList<Redemption>> ListForCardAsync(long cardId, int limit, CancellationToken ct)
    {
        await using var conn = _db.Create();
        var rows = await conn.QueryAsync<Redemption>(new CommandDefinition("""
            SELECT id, card_id, merchant_id, store_id, amount, currency, receipt_no, status, created_at
            FROM redemptions
            WHERE card_id = @cardId
            ORDER BY created_at DESC
            LIMIT @limit
            """, new { cardId, limit = Math.Clamp(limit, 1, 100) }, cancellationToken: ct));
        return rows.AsList();
    }

    public Task EnqueueNotificationAsync(DbConnection conn, DbTransaction? tx, long redemptionId, string payloadJson) =>
        conn.ExecuteAsync(
            "INSERT INTO notification_outbox (redemption_id, payload) VALUES (@redemptionId, CAST(@payloadJson AS jsonb))",
            new { redemptionId, payloadJson }, tx);

    public async Task<IReadOnlyList<(long Id, string Payload)>> GetPendingNotificationsAsync(int limit, CancellationToken ct)
    {
        await using var conn = _db.Create();
        var rows = await conn.QueryAsync<(long, string)>(new CommandDefinition("""
            SELECT id, payload::text FROM notification_outbox
            WHERE sent_at IS NULL AND attempts < 10
            ORDER BY id
            LIMIT @limit
            """, new { limit }, cancellationToken: ct));
        return rows.AsList();
    }

    public async Task MarkNotificationAsync(long id, bool sent, CancellationToken ct)
    {
        await using var conn = _db.Create();
        await conn.ExecuteAsync(new CommandDefinition(
            sent
                ? "UPDATE notification_outbox SET sent_at = now(), attempts = attempts + 1 WHERE id = @id"
                : "UPDATE notification_outbox SET attempts = attempts + 1 WHERE id = @id",
            new { id }, cancellationToken: ct));
    }
}
