using Lending.Servicing.Part4.Domain;

namespace Lending.Servicing.Part4.RateLocks;

/// <summary>Pricing rules for rate-lock extensions.</summary>
public static class RateLockPricing
{
    public const int MaxExtensionDays = 30;
    public const decimal BasisPointsPerDay = 2m;
    public const decimal MinimumFee = 25m;
    public const decimal MaximumFeeRatio = 0.005m;

    /// <summary>
    /// Fee for extending a lock: 2bp of the loan amount per day, with a floor
    /// and a cap of 0.5% of the loan amount. Extensions longer than the policy
    /// limit are rejected rather than silently truncated.
    /// </summary>
    public static decimal ExtensionFee(decimal loanAmount, RateLock currentLock, int extensionDays)
    {
        ArgumentNullException.ThrowIfNull(currentLock);

        if (loanAmount <= 0m)
        {
            throw new ArgumentOutOfRangeException(nameof(loanAmount), "Loan amount must be positive.");
        }

        if (extensionDays <= 0 || extensionDays > MaxExtensionDays)
        {
            throw new ArgumentOutOfRangeException(nameof(extensionDays), $"Extension must be between 1 and {MaxExtensionDays} days.");
        }

        var perDay = loanAmount * BasisPointsPerDay / 10_000m;
        var raw = perDay * extensionDays;

        var cap = loanAmount * MaximumFeeRatio;
        var bounded = Math.Min(Math.Max(raw, MinimumFee), Math.Max(cap, MinimumFee));

        return decimal.Round(bounded, 2, MidpointRounding.AwayFromZero);
    }
}
