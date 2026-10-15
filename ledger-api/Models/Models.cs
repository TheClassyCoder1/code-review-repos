using System.Text.Json.Serialization;

namespace LedgerApi.Models;

public sealed record Account(string Id, string OwnerId, string Currency, decimal Balance, decimal DailyLimit);

public sealed record LedgerEntry(long Id, string AccountId, decimal Amount, string Memo, DateTime PostedAt);

public sealed record UpdateLimitRequest(decimal DailyLimit);

public sealed record TransferRequest(decimal Amount, string Currency);

public sealed record AuditEvent(string Action, string AccountId, string ActorId, DateTime At);

public sealed record StatementLine(string Reference, decimal Amount, string Description);

public sealed class ImportBatch
{
    public string BatchId { get; set; } = "";
    public List<ImportEntry> Entries { get; set; } = new();
}

public sealed class ImportEntry
{
    public string ExternalId { get; set; } = "";
    public string AccountId { get; set; } = "";
    public decimal Amount { get; set; }
    public string Memo { get; set; } = "";
    public Dictionary<string, object>? Metadata { get; set; }
}

public enum EntrySort
{
    Posted,
    Amount,
    Memo,
}

public static class SortColumns
{
    public static string For(EntrySort sort) => sort switch
    {
        EntrySort.Amount => "Amount",
        EntrySort.Memo => "Memo",
        _ => "PostedAt",
    };
}

public sealed class BankTransferRequest
{
    public required string AccountId { get; init; }
    public required decimal Amount { get; init; }
    public required string Currency { get; init; }

    [JsonIgnore]
    public required string ApiKey { get; init; }

    public override string ToString() =>
        $"BankTransferRequest {{ AccountId = {AccountId}, Amount = {Amount}, Currency = {Currency} }}";
}
