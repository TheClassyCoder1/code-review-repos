using Dapper;
using GiftCards.Data;
using GiftCards.Models;

namespace GiftCards.Services;

public sealed class TransferService
{
    private readonly GiftCardRepository _cards;
    private readonly ILogger<TransferService> _logger;

    public TransferService(GiftCardRepository cards, ILogger<TransferService> logger)
    {
        _cards = cards;
        _logger = logger;
    }

    public static string? ValidateTransfer(GiftCard? from, GiftCard? to, string ownerId)
    {
        if (from is null || to is null)
        {
            return "Both cards must exist.";
        }
        if (from.Id == to.Id)
        {
            return "Cannot merge a card into itself.";
        }
        if (from.OwnerId != ownerId || to.OwnerId != ownerId)
        {
            return "Both cards must belong to the caller.";
        }
        if (from.Status != CardStatus.Active || to.Status != CardStatus.Active)
        {
            return "Both cards must be active.";
        }
        if (!string.Equals(from.Currency, to.Currency, StringComparison.OrdinalIgnoreCase))
        {
            return "Cards must share a currency.";
        }
        return from.Balance <= 0 ? "Source card has no balance." : null;
    }

    public async Task<(GiftCard? From, GiftCard? To)> LoadPairAsync(TransferRequest request, CancellationToken ct)
    {
        var from = await _cards.GetByCodeAsync(request.FromCode, ct);
        var to = await _cards.GetByCodeAsync(request.ToCode, ct);
        return (from, to);
    }

    public async Task<decimal> MergeCardsAsync(GiftCard from, GiftCard to, CancellationToken ct)
    {
        await using var conn = _cards.OpenConnection();
        await conn.OpenAsync(ct);

        var moved = await conn.ExecuteScalarAsync<decimal>("""
            UPDATE gift_cards g SET balance = 0, status = 'Replaced', replaced_by = @toId
            FROM (SELECT id, balance FROM gift_cards WHERE id = @fromId AND status = 'Active' FOR UPDATE) old
            WHERE g.id = old.id
            RETURNING old.balance
            """, new { fromId = from.Id, toId = to.Id });

        await conn.ExecuteAsync(
            "UPDATE gift_cards SET balance = balance + @moved WHERE id = @toId",
            new { moved, toId = to.Id });

        _logger.LogInformation("Merged card {FromId} into {ToId}", from.Id, to.Id);
        return to.Balance + moved;
    }
}
