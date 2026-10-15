using System.Net.Http.Json;

namespace Payroll.Services;

public sealed class BankHolidayClient
{
    private readonly HttpClient _http;

    public BankHolidayClient(HttpClient http)
    {
        _http = http;
    }

    public async Task<IReadOnlyList<DateOnly>> FetchHolidaysAsync(string host, int year, CancellationToken ct)
    {
        var url = $"https://{host}/calendar/holidays?year={year}";
        var dates = await _http.GetFromJsonAsync<List<DateOnly>>(url, ct);
        return dates ?? new List<DateOnly>();
    }

    private static bool IsWeekend(DateOnly date)
    {
        return date.DayOfWeek is DayOfWeek.Saturday or DayOfWeek.Sunday;
    }

    public static DateOnly NextBusinessDay(DateOnly date, IReadOnlyCollection<DateOnly> holidays)
    {
        var candidate = date;
        while (IsWeekend(candidate) || holidays.Contains(candidate))
        {
            candidate = candidate.AddDays(1);
        }
        return candidate;
    }
}
