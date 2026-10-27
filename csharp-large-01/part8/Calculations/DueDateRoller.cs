namespace Lending.Part8.Calculations;

public enum RollConvention
{
    Following,
    ModifiedFollowing,
    Preceding,
}

public sealed class DueDateRoller(IReadOnlySet<DateOnly> bankHolidays)
{
    public DateOnly Roll(DateOnly scheduled, RollConvention convention)
    {
        if (IsBusinessDay(scheduled))
        {
            return scheduled;
        }

        switch (convention)
        {
            case RollConvention.Preceding:
                return Step(scheduled, -1);
            case RollConvention.ModifiedFollowing:
                var following = Step(scheduled, 1);
                return following.Month == scheduled.Month ? following : Step(scheduled, -1);
            default:
                return Step(scheduled, 1);
        }
    }

    private DateOnly Step(DateOnly from, int direction)
    {
        var candidate = from.AddDays(direction);
        while (!IsBusinessDay(candidate))
        {
            candidate = candidate.AddDays(direction);
        }
        return candidate;
    }

    private bool IsBusinessDay(DateOnly date) =>
        date.DayOfWeek is not (DayOfWeek.Saturday or DayOfWeek.Sunday) && !bankHolidays.Contains(date);
}
