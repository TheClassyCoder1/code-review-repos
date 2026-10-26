using System.Globalization;
using Lending.Servicing.Part3.Domain;

namespace Lending.Servicing.Part3.Collections;

public sealed record ReminderLine(string LoanId, int InstallmentNumber, string DueDate, string AmountDue);

/// <summary>
/// Renders the "next payment" block of a reminder from the account schedule.
/// </summary>
public static class NextDueInstallmentFormatter
{
    private static readonly CultureInfo Invariant = CultureInfo.InvariantCulture;

    public static ReminderLine Format(LoanAccount account)
    {
        var next = account.Schedule
            .Where(i => i.Status == InstallmentStatus.Due)
            .OrderBy(i => i.DueDate)
            .FirstOrDefault();

        return new ReminderLine(
            account.LoanId,
            next!.Number,
            next.DueDate.ToString("yyyy-MM-dd", Invariant),
            next.Outstanding.ToString("0.00", Invariant));
    }
}
