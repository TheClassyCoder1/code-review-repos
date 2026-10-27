using PolicyAdmin.Models;

namespace PolicyAdmin.Services;

public static class PremiumCalculator
{
    private const decimal PremiumTaxRate = 0.0275m;
    private const decimal ShortRatePenalty = 0.10m;

    public static decimal ProrateEndorsement(Policy policy, DateOnly effective, decimal annualDelta)
    {
        var termDays = policy.ExpirationDate.DayNumber - policy.EffectiveDate.DayNumber;
        if (termDays <= 0 || effective >= policy.ExpirationDate)
        {
            return 0m;
        }
        var start = effective < policy.EffectiveDate ? policy.EffectiveDate : effective;
        var remainingDays = policy.ExpirationDate.DayNumber - start.DayNumber;
        return decimal.Round(annualDelta * remainingDays / termDays, 2, MidpointRounding.AwayFromZero);
    }

    public static decimal Taxes(decimal premium) =>
        decimal.Round(premium * PremiumTaxRate, 2, MidpointRounding.AwayFromZero);

    public static int TermDays(Policy policy) =>
        policy.ExpirationDate.DayNumber - policy.EffectiveDate.DayNumber;

    public static decimal CancellationRefund(Policy policy, DateOnly cancelDate, bool insuredRequested)
    {
        var termDays = TermDays(policy);
        if (termDays <= 0 || cancelDate >= policy.ExpirationDate)
        {
            return 0m;
        }
        var effectiveCancel = cancelDate < policy.EffectiveDate ? policy.EffectiveDate : cancelDate;
        var unearned = policy.AnnualPremium * (policy.ExpirationDate.DayNumber - effectiveCancel.DayNumber) / termDays;
        if (insuredRequested)
        {
            unearned *= 1m - ShortRatePenalty;
        }
        return decimal.Round(unearned, 2, MidpointRounding.ToZero);
    }

    public static decimal Commission(Agent agent, decimal writtenPremium) =>
        decimal.Round(writtenPremium * agent.CommissionRate, 2, MidpointRounding.AwayFromZero);

    public static bool IsLicenseCurrent(Agent agent, DateOnly asOf) => agent.LicenseExpires >= asOf;

    public static PremiumBreakdown Breakdown(Policy policy, IEnumerable<Endorsement> endorsements)
    {
        var endorsementTotal = 0m;
        foreach (var endorsement in endorsements)
        {
            endorsementTotal += ProrateEndorsement(policy, endorsement.EffectiveDate, endorsement.PremiumDelta);
        }
        var subtotal = policy.AnnualPremium + endorsementTotal;
        var taxes = Taxes(subtotal);
        return new PremiumBreakdown(policy.AnnualPremium, endorsementTotal, taxes, subtotal + taxes);
    }

    public static DateOnly NextRenewalDate(Policy policy) => policy.ExpirationDate;

    public static int DaysUntilRenewal(Policy policy, DateOnly today) =>
        Math.Max(0, policy.ExpirationDate.DayNumber - today.DayNumber);

    public static DateOnly RenewalNoticeDate(Policy policy, LineOfBusiness line)
    {
        var leadDays = line switch
        {
            LineOfBusiness.Commercial => 90,
            LineOfBusiness.Umbrella => 60,
            _ => 45
        };
        var notice = policy.ExpirationDate.AddDays(-leadDays);
        while (notice.DayOfWeek is DayOfWeek.Saturday or DayOfWeek.Sunday)
        {
            notice = notice.AddDays(-1);
        }
        return notice;
    }
}
