using System.ComponentModel.DataAnnotations;

namespace GiftCards.Models;

public enum CardStatus
{
    Inactive,
    Active,
    Cancelled,
    Expired,
    Replaced
}

public sealed class GiftCard
{
    public long Id { get; init; }
    public string Code { get; init; } = "";
    public int ProgrammeId { get; init; }
    public string ProgrammeName { get; init; } = "";
    public string OwnerId { get; init; } = "";
    public string Currency { get; init; } = "USD";
    public decimal Balance { get; init; }
    public CardStatus Status { get; init; }
    public long? BatchId { get; init; }
    public DateOnly ValidThrough { get; init; }
}

public sealed class Redemption
{
    public long Id { get; init; }
    public long? CardId { get; init; }
    public string? VoucherCode { get; init; }
    public string MerchantId { get; init; } = "";
    public string? StoreId { get; init; }
    public decimal Amount { get; init; }
    public string Currency { get; init; } = "USD";
    public string ReceiptNo { get; init; } = "";
    public string Status { get; init; } = "Completed";
    public string? ProcessorRef { get; init; }
    public DateTimeOffset CreatedAt { get; init; }
}

public sealed record Voucher(string Code, decimal FaceValue, DateOnly ValidThrough);

public sealed record ChannelInfo(
    [property: Required] string StoreId,
    [property: Required] string TerminalId);

public sealed record RedeemRequest(
    [property: Required] string CardCode,
    [property: Range(0.01, 100_000)] decimal Amount,
    [property: Required, StringLength(3, MinimumLength = 3)] string Currency,
    [property: Required] string MerchantId,
    [property: Required] ChannelInfo Channel);

public sealed record VoucherRedeemRequest(
    [property: Required] string VoucherCode,
    [property: Required] string MerchantId);

public sealed record TopUpRequest([property: Range(1, 2_000)] decimal Amount);

public sealed record IssueBatchRequest(
    [property: Range(1, 1_000)] int ProgrammeId,
    [property: Range(1, 500)] int Quantity,
    [property: Range(5, 2_000)] decimal FaceValue,
    [property: Required, StringLength(3, MinimumLength = 3)] string Currency);

public sealed record VoucherIssueRequest(long BatchId, int ProgrammeId, decimal FaceValue, string Currency, string RequestedBy);

public sealed record TransferRequest(
    [property: Required] string FromCode,
    [property: Required] string ToCode);

public sealed record BalanceView(string MaskedCode, decimal Balance, string Currency, DateOnly ValidThrough, string Status);

public sealed record Receipt(string ReceiptNo, string MaskedCode, decimal Amount, decimal RemainingBalance, string Currency, DateTimeOffset At);

public enum PromoTier
{
    None,
    Holiday,
    Corporate,
    Loyalty
}

public static class PromoTiers
{
    public const double HolidayBonus = 0.05;
    public const double CorporateBonus = 0.10;
    public const double LoyaltyBonus = 0.15;

    public static double BonusRate(PromoTier tier) => tier switch
    {
        PromoTier.Holiday => HolidayBonus,
        PromoTier.Corporate => CorporateBonus,
        PromoTier.Loyalty => LoyaltyBonus,
        _ => 0
    };
}
