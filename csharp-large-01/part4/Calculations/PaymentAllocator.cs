using Lending.Servicing.Part4.Domain;

namespace Lending.Servicing.Part4.Calculations;

/// <summary>
/// Applies an incoming payment using the regulatory waterfall: outstanding
/// fees first, then accrued interest, then principal. Anything left over is
/// held as unapplied funds rather than creating a negative balance.
/// </summary>
public static class PaymentAllocator
{
    public static PaymentAllocation Allocate(
        decimal paymentAmount,
        decimal feesDue,
        decimal interestDue,
        decimal principalOutstanding)
    {
        if (paymentAmount < 0m)
        {
            throw new ArgumentOutOfRangeException(nameof(paymentAmount), "Payment amount cannot be negative.");
        }

        if (feesDue < 0m || interestDue < 0m || principalOutstanding < 0m)
        {
            throw new ArgumentOutOfRangeException(nameof(feesDue), "Outstanding balances cannot be negative.");
        }

        var remaining = paymentAmount;

        var toFees = Math.Min(remaining, feesDue);
        remaining -= toFees;

        var toInterest = Math.Min(remaining, interestDue);
        remaining -= toInterest;

        var toPrincipal = Math.Min(remaining, principalOutstanding);
        remaining -= toPrincipal;

        return new PaymentAllocation(toFees, toInterest, toPrincipal, remaining);
    }
}
