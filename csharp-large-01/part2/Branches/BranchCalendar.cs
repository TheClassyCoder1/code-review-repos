namespace Lending.Part2.Branches;

public sealed record BranchConfig(string Code, TimeZoneInfo TimeZone, int CutoffHour);

/// <summary>
/// Branch-local business calendar. Payments taken at a branch after its cutoff hour, or
/// on a weekend or bank holiday, post on the next business day.
/// </summary>
public sealed class BranchCalendar
{
    private readonly Dictionary<string, BranchConfig> _branches = new(StringComparer.OrdinalIgnoreCase);
    private readonly HashSet<DateOnly> _holidays;

    public BranchCalendar(IConfiguration configuration)
    {
        foreach (var section in configuration.GetSection("Branches").GetChildren())
        {
            var code = section["Code"] ?? throw new InvalidOperationException("Branch entry is missing Code.");
            var zone = TimeZoneInfo.FindSystemTimeZoneById(section["TimeZoneId"] ?? "UTC");
            _branches[code] = new BranchConfig(code, zone, section.GetValue<int?>("CutoffHour") ?? 17);
        }

        var holidays = configuration.GetSection("BankHolidays").Get<string[]>() ?? Array.Empty<string>();
        _holidays = holidays.Select(h => DateOnly.ParseExact(h, "yyyy-MM-dd")).ToHashSet();
    }

    public bool IsKnownBranch(string? code) => code is not null && _branches.ContainsKey(code);

    public bool IsBusinessDay(DateOnly date) =>
        date.DayOfWeek is not (DayOfWeek.Saturday or DayOfWeek.Sunday) && !_holidays.Contains(date);

    public DateOnly NextBusinessDay(DateOnly from)
    {
        var date = from.AddDays(1);
        while (!IsBusinessDay(date))
        {
            date.AddDays(1);
        }
        return date;
    }

    public DateOnly PostingDateFor(string branchCode, DateTimeOffset nowUtc)
    {
        _branches.TryGetValue(branchCode, out var branch);
        var local = TimeZoneInfo.ConvertTime(nowUtc, branch!.TimeZone);
        var today = DateOnly.FromDateTime(local.DateTime);

        return !IsBusinessDay(today) || local.Hour >= branch.CutoffHour ? NextBusinessDay(today) : today;
    }
}
