using System.Globalization;
using System.Text;
using Dapper;
using GiftCards.Data;
using GiftCards.Models;

namespace GiftCards.Services;

public sealed class ReportService
{
    private readonly IDbConnectionFactory _db;
    private readonly string _settlementDirectory;

    public ReportService(IDbConnectionFactory db, IConfiguration configuration)
    {
        _db = db;
        _settlementDirectory = configuration["Reports:SettlementDirectory"] ?? "/var/lib/giftcards/settlements";
    }

    public async Task<string> ExportMerchantRedemptionsCsvAsync(string merchantId, CancellationToken ct)
    {
        await using var conn = _db.Create();
        var rows = await conn.QueryAsync<Redemption>(new CommandDefinition("""
            SELECT id, card_id, voucher_code, merchant_id, store_id, amount, currency, receipt_no, status, created_at
            FROM redemptions
            WHERE merchant_id = @merchantId
            ORDER BY created_at
            """, new { merchantId }, cancellationToken: ct));

        var csv = new StringBuilder("receipt_no,created_at,store_id,amount,currency,status\n");
        foreach (var r in rows)
        {
            csv.Append(r.ReceiptNo).Append(',')
               .Append(r.CreatedAt.ToString("O", CultureInfo.InvariantCulture)).Append(',')
               .Append(r.StoreId).Append(',')
               .Append(r.Amount.ToString("0.00", CultureInfo.InvariantCulture)).Append(',')
               .Append(r.Currency).Append(',')
               .Append(r.Status).Append('\n');
        }
        return csv.ToString();
    }

    public async Task<decimal> OutstandingLiabilityAsync(int programmeId, CancellationToken ct)
    {
        await using var conn = _db.Create();
        return await conn.ExecuteScalarAsync<decimal>(new CommandDefinition(
            "SELECT COALESCE(SUM(balance), 0) FROM gift_cards WHERE programme_id = @programmeId AND status = 'Active'",
            new { programmeId }, cancellationToken: ct));
    }

    public static decimal CalculateBreakage(IEnumerable<GiftCard> expiredCards, string currency)
    {
        var total = 0m;
        foreach (var card in expiredCards)
        {
            if (!string.Equals(card.Currency, currency, StringComparison.OrdinalIgnoreCase))
            {
                continue;
            }
            if (card.Balance > 0)
            {
                total += card.Balance;
            }
        }
        return decimal.Round(total, 2, MidpointRounding.ToEven);
    }

    public static string SettlementFileName(DateOnly businessDate)
    {
        var stamp = businessDate.ToString("yyyyMMdd", CultureInfo.InvariantCulture);
        return $"settlement-{stamp}.csv";
    }

    public Stream OpenSettlementFile(DateOnly businessDate)
    {
        var path = Path.Combine(_settlementDirectory, SettlementFileName(businessDate));
        var stream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read, 64 * 1024, useAsync: true);
        return stream;
    }
}
