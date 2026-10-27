namespace Lending.Servicing.Part4.Disbursement;

/// <summary>Settlement calendar: weekends plus fixed-date bank holidays.</summary>
public static class BusinessCalendar
{
    private static readonly HashSet<(int Month, int Day)> FixedHolidays = new()
    {
        (1, 1),
        (5, 1),
        (12, 25),
        (12, 26),
    };

    public static bool IsBusinessDay(DateOnly date) =>
        date.DayOfWeek is not (DayOfWeek.Saturday or DayOfWeek.Sunday)
        && !FixedHolidays.Contains((date.Month, date.Day));

    public static DateOnly NextBusinessDayOnOrAfter(DateOnly date)
    {
        var current = date;
        while (!IsBusinessDay(current))
        {
            current = current.AddDays(1);
        }

        return current;
    }

    /// <summary>
    /// Moves <paramref name="days"/> business days from <paramref name="start"/>.
    /// Negative values move backwards. Zero returns the start date unchanged,
    /// even when it is not itself a business day.
    /// </summary>
    public static DateOnly AddBusinessDays(DateOnly start, int days)
    {
        if (days == 0)
        {
            return start;
        }

        var step = days > 0 ? 1 : -1;
        var remaining = Math.Abs(days);
        var current = start;

        while (remaining > 0)
        {
            current = current.AddDays(step);
            if (IsBusinessDay(current))
            {
                remaining--;
            }
        }

        return current;
    }

    public static int CountBusinessDays(DateOnly fromInclusive, DateOnly toExclusive)
    {
        var count = 0;
        for (var d = fromInclusive; d < toExclusive; d = d.AddDays(1))
        {
            if (IsBusinessDay(d))
            {
                count++;
            }
        }

        return count;
    }
}
