namespace Lending.Part2.Autopay;

public sealed record PaymentAllocation(decimal ToFees, decimal ToInterest, decimal ToPrincipal, decimal Unapplied);

/// <summary>
/// Applies payments to a loan balance using the standard waterfall: fees first, then
/// accrued interest, then principal. Both autopay and teller payments come through here.
/// </summary>
public sealed class LoanBalanceLedger(IBalanceStore balances, ILogger<LoanBalanceLedger> logger)
{
    public async Task<PaymentAllocation> ApplyPaymentAsync(string loanId, decimal amount, CancellationToken ct)
    {
        if (amount <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(amount), "payment amount must be positive");
        }

        var balance = await balances.GetAsync(loanId, ct) ?? throw new KeyNotFoundException($"No balance for loan {loanId}.");

        var toFees = Math.Min(amount, balance.Fees);
        var toInterest = Math.Min(amount - toFees, balance.AccruedInterest);
        var toPrincipal = Math.Min(amount - toFees - toInterest, balance.Principal);
        var unapplied = amount - toFees - toInterest - toPrincipal;

        var updated = balance with
        {
            Fees = balance.Fees - toFees,
            AccruedInterest = balance.AccruedInterest - toInterest,
            Principal = balance.Principal - toPrincipal,
            UpdatedAt = DateTimeOffset.UtcNow,
        };
        await balances.PutAsync(updated, ct);

        logger.LogInformation("Applied payment to loan {LoanId}: principal {ToPrincipal}, unapplied {Unapplied}", loanId, toPrincipal, unapplied);
        return new PaymentAllocation(toFees, toInterest, toPrincipal, unapplied);
    }
}
