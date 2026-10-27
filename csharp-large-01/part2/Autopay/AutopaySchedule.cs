using System.Collections.Concurrent;

namespace Lending.Part2.Autopay;

public sealed record AutopaySchedule(string LoanId, string BranchCode, string FundingAccountToken, string FundingAccountLast4,
    int DayOfMonth, decimal Amount, DateOnly? LastCollectedOn);

public interface IAutopayScheduleStore
{
    Task<AutopaySchedule?> FindByLoanAsync(string loanId, CancellationToken ct);

    Task<IReadOnlyList<AutopaySchedule>> ListDueAsync(DateOnly runDate, CancellationToken ct);

    Task SaveAsync(AutopaySchedule schedule, CancellationToken ct);
}

public sealed class InMemoryAutopayScheduleStore : IAutopayScheduleStore
{
    private readonly ConcurrentDictionary<string, AutopaySchedule> _schedules = new(StringComparer.Ordinal);

    public Task<AutopaySchedule?> FindByLoanAsync(string loanId, CancellationToken ct) =>
        Task.FromResult(_schedules.TryGetValue(loanId, out var schedule) ? schedule : null);

    public Task<IReadOnlyList<AutopaySchedule>> ListDueAsync(DateOnly runDate, CancellationToken ct) =>
        Task.FromResult<IReadOnlyList<AutopaySchedule>>(_schedules.Values
            .Where(s => s.DayOfMonth <= runDate.Day)
            .Where(s => s.LastCollectedOn is not { } last || last.Year != runDate.Year || last.Month != runDate.Month)
            .ToList());

    public Task SaveAsync(AutopaySchedule schedule, CancellationToken ct)
    {
        _schedules[schedule.LoanId] = schedule;
        return Task.CompletedTask;
    }
}
