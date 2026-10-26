namespace Lending.Servicing.Part3.Domain;

public enum InstallmentStatus
{
    Scheduled,
    Due,
    Paid,
    Waived
}

public enum DelinquencyStage
{
    Current,
    EarlyArrears,
    LateArrears,
    ChargeOff
}

public sealed class Installment
{
    public int Number { get; init; }
    public DateOnly DueDate { get; init; }
    public decimal Principal { get; init; }
    public decimal Interest { get; init; }
    public decimal AmountPaid { get; set; }
    public InstallmentStatus Status { get; set; } = InstallmentStatus.Scheduled;

    public decimal Total => Principal + Interest;
    public decimal Outstanding => Math.Max(0m, Total - AmountPaid);
}

/// <summary>
/// Servicing-side view of a loan. Owned by the account store; callers mutate a copy
/// and save it back.
/// </summary>
public sealed class LoanAccount
{
    public string LoanId { get; init; } = string.Empty;
    public string ProductCode { get; init; } = string.Empty;
    public decimal PrincipalBalance { get; set; }
    public decimal AccruedInterest { get; set; }
    public decimal FeesOutstanding { get; set; }
    public decimal AnnualRate { get; init; }
    public DateOnly? LastAccrualDate { get; set; }
    public DateOnly? LastPaymentDate { get; set; }
    public long Version { get; set; }
    public List<Installment> Schedule { get; init; } = new();

    public decimal TotalOutstanding => PrincipalBalance + AccruedInterest + FeesOutstanding;

    public LoanAccount Clone() => new()
    {
        LoanId = LoanId,
        ProductCode = ProductCode,
        PrincipalBalance = PrincipalBalance,
        AccruedInterest = AccruedInterest,
        FeesOutstanding = FeesOutstanding,
        AnnualRate = AnnualRate,
        LastAccrualDate = LastAccrualDate,
        LastPaymentDate = LastPaymentDate,
        Version = Version,
        Schedule = Schedule.Select(i => new Installment
        {
            Number = i.Number,
            DueDate = i.DueDate,
            Principal = i.Principal,
            Interest = i.Interest,
            AmountPaid = i.AmountPaid,
            Status = i.Status
        }).ToList()
    };
}

public sealed record DelinquencyCase(
    string CaseId, string LoanId, DelinquencyStage Stage, int DaysPastDue, DateTimeOffset OpenedAt);

public sealed record RemittanceLine(
    string ReferenceId, string LoanId, decimal Amount, DateOnly ValueDate, string Channel);

public sealed record PaymentReceipt(
    string ReceiptId, string LoanId, decimal Amount,
    decimal AppliedToFees, decimal AppliedToInterest, decimal AppliedToPrincipal, DateTimeOffset PostedAt);

public sealed record LedgerEntry(
    string EntryId, string LoanId, string Account, decimal Debit, decimal Credit, DateTimeOffset PostedAt);

public sealed record DunningNotice(
    string NoticeId, string LoanId, DelinquencyStage Stage, string TemplateCode, DateTimeOffset CreatedAt);

public sealed record RateCard(
    string ProductCode, decimal BaseRate, decimal LateFeeFlat, decimal LateFeePercent, int GraceDays, DateTimeOffset FetchedAt);

public sealed record PartnerLoanRecord(string PartnerLoanId, string LoanId, decimal Balance, string Status);

public sealed record PartnerLoanPage(IReadOnlyList<PartnerLoanRecord> Items, string? NextCursor);

public sealed record BureauScore(string LoanId, int Score, string Band, DateTimeOffset RetrievedAt);

public sealed record AgencyAssignment(string AssignmentId, string LoanId, string AgencyCode, DateTimeOffset AssignedAt);

public sealed record WebhookDelivery(string DeliveryId, string PartnerCode, Uri Target, string Payload);

public sealed record StatementRequest(string LoanId, DateOnly PeriodStart, DateOnly PeriodEnd);
