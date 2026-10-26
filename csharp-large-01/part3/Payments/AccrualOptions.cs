namespace Lending.Servicing.Part3.Payments;

/// <summary>
/// Bound from the <c>Servicing:Accrual</c> configuration section. The setter normalizes the
/// value so a bad deployment setting degrades to daily accrual instead of breaking the job.
/// </summary>
public sealed class AccrualOptions
{
    private int _periodDays = 1;

    /// <summary>Length of one accrual step in days. Always within [1, 31].</summary>
    public int PeriodDays
    {
        get => _periodDays;
        set => _periodDays = Math.Clamp(value, 1, 31);
    }
}
