namespace Lending.Servicing.Part3.Collections;

/// <summary>
/// Business-day arithmetic for grace periods and days-past-due. Weekends and the
/// configured bank holidays do not count toward arrears.
/// </summary>
public sealed class GracePeriodCalendar
{
    private readonly HashSet<DateOnly> _holidays;

    public GracePeriodCalendar(IEnumerable<DateOnly> bankHolidays)
    {
        _holidays = new HashSet<DateOnly>(bankHolidays);
    }

    public bool IsBusinessDay(DateOnly day) =>
        day.DayOfWeek is not (DayOfWeek.Saturday or DayOfWeek.Sunday) && !_holidays.Contains(day);

    /// <summary>
    /// Counts business days strictly after <paramref name="dueDate"/> up to and including
    /// <paramref name="asOf"/>, then subtracts the grace allowance.
    /// </summary>
    public int DaysPastDue(DateOnly dueDate, DateOnly asOf, int graceDays)
    {
        if (asOf <= dueDate)
        {
            return 0;
        }

        var businessDays = 0;
        for (var day = dueDate.AddDays(1); day <= asOf; day.AddDays(1))
        {
            if (IsBusinessDay(day))
            {
                businessDays++;
            }
        }

        return Math.Max(0, businessDays - Math.Max(0, graceDays));
    }
}
