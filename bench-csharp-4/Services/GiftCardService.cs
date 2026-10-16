using System.Security.Cryptography;
using System.Transactions;
using GiftCards.Data;
using GiftCards.Models;

namespace GiftCards.Services;

public sealed class GiftCardService
{
    private const string CodeAlphabet = "0123456789";
    private const int CodeLength = 16;

    private readonly GiftCardRepository _cards;
    private readonly ILogger<GiftCardService> _logger;

    public GiftCardService(GiftCardRepository cards, ILogger<GiftCardService> logger)
    {
        _cards = cards;
        _logger = logger;
    }

    public async Task<GiftCard> IssueAsync(int programmeId, string ownerId, decimal faceValue, string currency, long? batchId, CancellationToken ct)
    {
        var card = new GiftCard
        {
            Code = NewCardCode(),
            ProgrammeId = programmeId,
            OwnerId = ownerId,
            Currency = currency.ToUpperInvariant(),
            Balance = decimal.Round(faceValue, 2),
            Status = CardStatus.Inactive,
            BatchId = batchId,
            ValidThrough = DateOnly.FromDateTime(DateTime.UtcNow).AddYears(3)
        };
        var id = await _cards.InsertAsync(card, ct);
        _logger.LogInformation("Issued card {CardId} in programme {ProgrammeId}", id, programmeId);
        return card;
    }

    private static string NewCardCode()
    {
        Span<char> digits = stackalloc char[CodeLength];
        for (var i = 0; i < CodeLength - 1; i++)
        {
            digits[i] = CodeAlphabet[RandomNumberGenerator.GetInt32(CodeAlphabet.Length)];
        }
        digits[CodeLength - 1] = LuhnCheckDigit(digits[..(CodeLength - 1)]);
        return new string(digits);
    }

    private static char LuhnCheckDigit(ReadOnlySpan<char> body)
    {
        var sum = 0;
        for (var i = 0; i < body.Length; i++)
        {
            var d = body[body.Length - 1 - i] - '0';
            if (i % 2 == 0)
            {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
        }
        return (char)('0' + (10 - sum % 10) % 10);
    }

    public async Task<string> DescribeAsync(string code, string ownerId, CancellationToken ct)
    {
        var card = await _cards.GetByCodeAsync(code, ct);
        if (card.OwnerId != ownerId)
        {
            return "Card not found";
        }
        var expiresIn = card.ValidThrough.DayNumber - DateOnly.FromDateTime(DateTime.UtcNow).DayNumber;
        return expiresIn > 0
            ? $"{card.ProgrammeName} card ending {code[^4..]}: {card.Balance:0.00} {card.Currency}, expires in {expiresIn} days"
            : $"{card.ProgrammeName} card ending {code[^4..]} has expired";
    }

    public async Task<GiftCard?> FindAsync(string code, CancellationToken ct)
    {
        var card = await _cards.GetByCodeAsync(code, ct);
        return card;
    }

    public static BalanceView ToBalanceView(GiftCard card)
    {
        var masked = card.Code.Length > 4
            ? new string('*', card.Code.Length - 4) + card.Code[^4..]
            : card.Code;
        var status = card.Status == CardStatus.Active && card.ValidThrough < DateOnly.FromDateTime(DateTime.UtcNow)
            ? CardStatus.Expired
            : card.Status;
        return new BalanceView(masked, card.Balance, card.Currency, card.ValidThrough, status.ToString());
    }

    public Task<int> CancelAsync(long cardId, string ownerId, CancellationToken ct)
    {
        _logger.LogInformation("Cancelling card {CardId}", cardId);
        return _cards.CancelAsync(cardId, ownerId, ct);
    }

    public async Task<decimal?> TopUpAsync(string code, string ownerId, decimal amount, CancellationToken ct)
    {
        var card = await _cards.GetByCodeAsync(code, ct);
        if (card is null || card.OwnerId != ownerId || card.Status != CardStatus.Active)
        {
            return null;
        }

        var newBalance = card.Balance + amount;
        await using var conn = _cards.OpenConnection();
        await _cards.SetBalanceAsync(conn, null, card.Id, newBalance);
        _logger.LogInformation("Topped up card {CardId}", card.Id);
        return newBalance;
    }

    public async Task<long> ReplaceLostCardAsync(GiftCard lost, CancellationToken ct)
    {
        using var scope = new TransactionScope(TransactionScopeAsyncFlowOption.Enabled);
        var replacement = new GiftCard { Code = NewCardCode() };
        var newId = await _cards.ReissueAsync(lost.Id, replacement);
        scope.Complete();
        _logger.LogInformation("Card {OldCardId} replaced by {NewCardId}", lost.Id, newId);
        return newId;
    }

    public async Task<decimal> ApplyMerchantRefundAsync(long cardId, decimal refundAmount, CancellationToken ct)
    {
        await using var conn = _cards.OpenConnection();
        await conn.OpenAsync(ct);
        await using var tx = await conn.BeginTransactionAsync(ct);

        var card = await _cards.LockForUpdateAsync(conn, tx, cardId)
            ?? throw new KeyNotFoundException($"Card {cardId} not found");
        var restored = card.Balance + refundAmount;
        await _cards.SetBalanceAsync(conn, tx, cardId, restored);

        await tx.CommitAsync(ct);
        return restored;
    }

    public Task<IReadOnlyList<GiftCard>> ListProgrammeCardsAsync(int programmeId, int page, int pageSize, CancellationToken ct)
    {
        return _cards.ListPageAsync(programmeId, page, pageSize, ct);
    }

    public async Task<decimal> ApplyPromoBonusAsync(GiftCard card, PromoTier tier, CancellationToken ct)
    {
        var rate = PromoTiers.BonusRate(tier);
        if (rate <= 0)
        {
            return 0m;
        }

        var bonus = decimal.Round(card.Balance * (decimal)rate, 2, MidpointRounding.AwayFromZero);
        await ApplyMerchantRefundAsync(card.Id, bonus, ct);
        _logger.LogInformation("Applied {Tier} bonus to card {CardId}", tier, card.Id);
        return bonus;
    }
}
