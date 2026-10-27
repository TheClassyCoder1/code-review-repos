namespace Lending.Part8.Calculations;

public sealed record ScheduledInstallment(int Number, DateOnly DueDate, decimal AmountDue, decimal AmountPaid);

public static class DaysPastDueCalculator
{
    /// <summary>
    /// Days since the oldest installment that is still short by more than the tolerance.
    /// Returns 0 when the loan is current.
    /// </summary>
    public static int Compute(IEnumerable<ScheduledInstallment> installments, DateOnly asOf, decimal tolerance = 0.50m)
    {
        ArgumentNullException.ThrowIfNull(installments);
        if (tolerance < 0)
        {
            throw new ArgumentOutOfRangeException(nameof(tolerance));
        }

        DateOnly? oldestUnpaid = null;
        foreach (var installment in installments)
        {
            if (installment.DueDate >= asOf)
            {
                continue;
            }

            var shortfall = installment.AmountDue - installment.AmountPaid;
            if (shortfall > tolerance && (oldestUnpaid is null || installment.DueDate < oldestUnpaid))
            {
                oldestUnpaid = installment.DueDate;
            }
        }

        return oldestUnpaid is { } due ? asOf.DayNumber - due.DayNumber : 0;
    }
}
