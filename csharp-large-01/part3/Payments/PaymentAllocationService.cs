using Lending.Servicing.Part3.Domain;
using Lending.Servicing.Part3.Infrastructure;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Payments;

/// <summary>
/// Applies an incoming payment to a loan using the standard waterfall:
/// fees first, then accrued interest, then principal, then the oldest due installments.
/// </summary>
public sealed class PaymentAllocationService
{
    private readonly IAccountStore _accounts;
    private readonly TimeProvider _clock;
    private readonly ILogger<PaymentAllocationService> _logger;

    public PaymentAllocationService(IAccountStore accounts, TimeProvider clock, ILogger<PaymentAllocationService> logger)
    {
        _accounts = accounts;
        _clock = clock;
        _logger = logger;
    }

    public async Task<PaymentReceipt?> AllocateAsync(RemittanceLine line, CancellationToken cancellationToken)
    {
        if (line.Amount <= 0m)
        {
            throw new ArgumentOutOfRangeException(nameof(line), "Remittance amount must be positive.");
        }

        var account = await _accounts.GetAsync(line.LoanId, cancellationToken);
        if (account is null)
        {
            _logger.LogWarning("Remittance {ReferenceId} references unknown loan {LoanId}", line.ReferenceId, line.LoanId);
            return null;
        }

        var remaining = line.Amount;

        var toFees = Math.Min(remaining, account.FeesOutstanding);
        account.FeesOutstanding -= toFees;
        remaining -= toFees;

        var toInterest = Math.Min(remaining, account.AccruedInterest);
        account.AccruedInterest -= toInterest;
        remaining -= toInterest;

        var toPrincipal = Math.Min(remaining, account.PrincipalBalance);
        account.PrincipalBalance -= toPrincipal;
        remaining -= toPrincipal;

        ApplyToInstallments(account, line.Amount - remaining);
        account.LastPaymentDate = line.ValueDate;

        await _accounts.SaveAsync(account, cancellationToken);

        return new PaymentReceipt(
            ReceiptId: $"RCPT-{line.ReferenceId}",
            LoanId: line.LoanId,
            Amount: line.Amount - remaining,
            AppliedToFees: toFees,
            AppliedToInterest: toInterest,
            AppliedToPrincipal: toPrincipal,
            PostedAt: _clock.GetUtcNow());
    }

    private static void ApplyToInstallments(LoanAccount account, decimal applied)
    {
        foreach (var installment in account.Schedule
                     .Where(i => i.Status == InstallmentStatus.Due)
                     .OrderBy(i => i.DueDate))
        {
            if (applied <= 0m)
            {
                break;
            }

            var portion = Math.Min(applied, installment.Outstanding);
            installment.AmountPaid += portion;
            applied -= portion;

            if (installment.Outstanding == 0m)
            {
                installment.Status = InstallmentStatus.Paid;
            }
        }
    }
}
