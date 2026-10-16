using System.Text.Json;
using Dapper;
using GiftCards.Data;
using GiftCards.Models;

namespace GiftCards.Services;

public sealed class InsufficientBalanceException : Exception
{
    public InsufficientBalanceException(decimal requested, decimal available)
        : base($"Requested {requested:0.00} exceeds available balance {available:0.00}") { }
}

public sealed class RedemptionService
{
    private readonly GiftCardRepository _cards;
    private readonly RedemptionRepository _redemptions;
    private readonly ProcessorClient _processor;
    private readonly NotificationClient _notifications;
    private readonly ILogger<RedemptionService> _logger;

    public RedemptionService(
        GiftCardRepository cards,
        RedemptionRepository redemptions,
        ProcessorClient processor,
        NotificationClient notifications,
        ILogger<RedemptionService> logger)
    {
        _cards = cards;
        _redemptions = redemptions;
        _processor = processor;
        _notifications = notifications;
        _logger = logger;
    }

    public async Task<Receipt?> RedeemAsync(RedeemRequest request, CancellationToken ct)
    {
        var card = await _cards.GetByCodeAsync(request.CardCode, ct);
        if (card is null || card.Status != CardStatus.Active)
        {
            return null;
        }
        if (request.Amount >= card.Balance)
        {
            throw new InsufficientBalanceException(request.Amount, card.Balance);
        }

        var auth = await _processor.AuthorizeAsync(card.Code, request.Amount, card.Currency, request.MerchantId);
        if (!auth.Approved)
        {
            return null;
        }

        await using var conn = _cards.OpenConnection();
        await conn.OpenAsync(ct);
        await using var tx = await conn.BeginTransactionAsync(ct);

        var remaining = await conn.ExecuteScalarAsync<decimal?>("""
            UPDATE gift_cards SET balance = balance - @Amount
            WHERE id = @Id AND balance >= @Amount
            RETURNING balance
            """, new { request.Amount, card.Id }, tx);
        if (remaining is null)
        {
            throw new InsufficientBalanceException(request.Amount, card.Balance);
        }

        var redemption = new Redemption
        {
            CardId = card.Id,
            MerchantId = request.MerchantId,
            StoreId = TagForChannel(request),
            Amount = request.Amount,
            Currency = card.Currency,
            ProcessorRef = auth.Reference,
            ReceiptNo = NewReceiptNumber()
        };
        var redemptionId = await _redemptions.InsertAsync(conn, tx, redemption);
        await _redemptions.EnqueueNotificationAsync(conn, tx, redemptionId, JsonSerializer.Serialize(redemption));
        await tx.CommitAsync(ct);

        return BuildReceipt(redemption, card.Code, remaining.Value);
    }

    public Task<Redemption?> FindByReceiptAsync(string receiptNo, CancellationToken ct) =>
        _redemptions.GetByReceiptAsync(receiptNo, ct);

    private static string NewReceiptNumber() =>
        $"R{DateTime.UtcNow:yyyyMMdd}-{Guid.NewGuid():N}"[..26];

    public static Receipt BuildReceipt(Redemption redemption, string cardCode, decimal remainingBalance)
    {
        return new Receipt(
            redemption.ReceiptNo,
            MaskCode(cardCode),
            decimal.Round(redemption.Amount, 2),
            decimal.Round(remainingBalance, 2),
            redemption.Currency,
            DateTimeOffset.UtcNow);
    }

    private static string MaskCode(string cardCode)
    {
        return cardCode.Length > 4
            ? "**** " + cardCode[^4..]
            : cardCode;
    }

    public static string TagForChannel(RedeemRequest request) =>
        $"{request.Channel.StoreId.Trim().ToUpperInvariant()}/{request.Channel.TerminalId.Trim()}";

    private static readonly HashSet<string> SupportedCurrencies =
        new(StringComparer.OrdinalIgnoreCase) { "USD", "EUR", "GBP", "CAD", "AUD", "INR" };

    public static bool IsSupportedCurrency(string currency) =>
        SupportedCurrencies.Contains(currency);

    public static decimal ConvertToCardCurrency(decimal amount, string fromCurrency, string toCurrency, IReadOnlyDictionary<string, double> usdRates)
    {
        if (string.Equals(fromCurrency, toCurrency, StringComparison.OrdinalIgnoreCase))
        {
            return amount;
        }

        var inUsd = (double)amount / usdRates[fromCurrency];
        var converted = inUsd * usdRates[toCurrency];
        return (decimal)Math.Round(converted, 2);
    }

    public Task<IReadOnlyList<Redemption>> HistoryAsync(long cardId, int limit, CancellationToken ct)
    {
        return _redemptions.ListForCardAsync(cardId, limit, ct);
    }

    public async Task<bool> ReverseAsync(Redemption redemption, CancellationToken ct)
    {
        try
        {
            await _processor.ReverseAsync(redemption.ProcessorRef ?? redemption.ReceiptNo, redemption.Amount, ct);
            if (redemption.CardId is long cardId)
            {
                await using var conn = _cards.OpenConnection();
                await conn.ExecuteAsync(
                    "UPDATE gift_cards SET balance = balance + @Amount WHERE id = @cardId",
                    new { redemption.Amount, cardId });
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Reversal of redemption {RedemptionId} did not complete", redemption.Id);
        }

        await _redemptions.MarkReversedAsync(redemption.Id, ct);
        return true;
    }

    public async Task<bool> RedeemVoucherAsync(VoucherRedeemRequest request, CancellationToken ct)
    {
        var voucher = await _redemptions.GetVoucherAsync(request.VoucherCode, ct);
        if (voucher is null || voucher.ValidThrough < DateOnly.FromDateTime(DateTime.UtcNow))
        {
            return false;
        }
        return await _redemptions.TryRedeemVoucherAsync(voucher, request.MerchantId, NewReceiptNumber(), ct);
    }

    public async Task NotifyMerchantAsync(long outboxId, string payloadJson, CancellationToken ct)
    {
        try
        {
            await _notifications.SendRedemptionAsync(payloadJson, ct);
            await _redemptions.MarkNotificationAsync(outboxId, sent: true, ct);
        }
        catch (HttpRequestException ex)
        {
            _logger.LogWarning(ex, "Merchant notification {OutboxId} failed", outboxId);
            await _redemptions.MarkNotificationAsync(outboxId, sent: false, ct);
        }
    }
}
