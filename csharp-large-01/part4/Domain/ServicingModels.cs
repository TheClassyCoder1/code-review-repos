namespace Lending.Servicing.Part4.Domain;

/// <summary>A serviced loan as seen by the back-office servicing jobs.</summary>
public sealed record LoanAccount(
    string LoanId,
    string AccountId,
    decimal Principal,
    decimal AnnualRate,
    int TermMonths,
    DateOnly FirstPaymentDate,
    string CurrencyCode);

public sealed record ScheduleRow(
    int Number,
    DateOnly DueDate,
    decimal Payment,
    decimal Interest,
    decimal Principal,
    decimal RemainingBalance);

public enum LedgerEntryKind
{
    Disbursement,
    Payment,
    Interest,
    Fee,
    Adjustment,
}

public sealed record LedgerEntry(
    string EntryId,
    string LoanId,
    DateOnly PostedOn,
    LedgerEntryKind Kind,
    decimal Amount);

public sealed record StatementLine(DateOnly Date, string Description, decimal Amount);

public sealed record Statement(
    string StatementId,
    string LoanId,
    DateOnly PeriodStart,
    DateOnly PeriodEnd,
    decimal OpeningBalance,
    IReadOnlyList<StatementLine> Lines);

public sealed record StatementSummary(
    decimal OpeningBalance,
    decimal TotalCharges,
    decimal TotalCredits,
    decimal ClosingBalance,
    int LineCount);

public sealed record PaymentAllocation(
    decimal ToFees,
    decimal ToInterest,
    decimal ToPrincipal,
    decimal Unapplied);

public sealed record RateLock(
    string LoanId,
    decimal LockedRate,
    DateTimeOffset LockedAt,
    DateTimeOffset ExpiresAt);

public sealed record DisbursementRequest(
    string RequestId,
    string LoanId,
    string DestinationIban,
    decimal Amount,
    string CurrencyCode,
    DateOnly RequestedDate);

public sealed record BureauReport(
    string LoanId,
    string BureauReference,
    int Score,
    DateTimeOffset RetrievedAt);

public enum DelinquencyBucket
{
    Current,
    Days1To29,
    Days30To59,
    Days60To89,
    Days90Plus,
    ChargedOff,
}

public sealed record ReconciliationResult(
    string LoanId,
    decimal LedgerBalance,
    decimal StatementBalance,
    IReadOnlyList<string> MissingFromStatement,
    IReadOnlyList<string> UnknownOnStatement)
{
    public bool IsBalanced =>
        LedgerBalance == StatementBalance
        && MissingFromStatement.Count == 0
        && UnknownOnStatement.Count == 0;
}
