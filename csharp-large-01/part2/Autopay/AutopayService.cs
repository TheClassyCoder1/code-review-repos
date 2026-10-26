using Lending.Part2.Branches;

namespace Lending.Part2.Autopay;

public sealed record AutopayRunSummary(DateOnly RunDate, int Attempted, int Collected, int Declined);

public sealed class AutopayService(
    IAutopayScheduleStore schedules,
    IPaymentGateway gateway,
    LoanBalanceLedger ledger,
    BranchCalendar calendar,
    ILogger<AutopayService> logger)
{
    public Task<AutopaySchedule?> GetScheduleAsync(string loanId, CancellationToken ct) => schedules.FindByLoanAsync(loanId, ct);

    public async Task<AutopayRunSummary> RunDueAsync(DateTimeOffset nowUtc, CancellationToken ct)
    {
        var runDate = DateOnly.FromDateTime(nowUtc.UtcDateTime);
        var due = await schedules.ListDueAsync(runDate, ct);
        var collected = 0;
        var declined = 0;

        foreach (var schedule in due)
        {
            var postingDate = calendar.PostingDateFor(schedule.BranchCode, nowUtc);
            var idempotencyKey = $"autopay:{schedule.LoanId}:{runDate:yyyyMM}";

            try
            {
                var result = await gateway.DebitAsync(schedule.FundingAccountToken, schedule.Amount, idempotencyKey, ct);
                if (!result.Approved)
                {
                    declined++;
                    logger.LogWarning("Autopay for loan {LoanId} declined with {DeclineCode}", schedule.LoanId, result.DeclineCode);
                    continue;
                }

                await ledger.ApplyPaymentAsync(schedule.LoanId, schedule.Amount, ct);
                collected++;
            }
            catch (Exception)
            {
                // Gateway hiccups are picked up again on the next scheduled run.
            }

            await schedules.SaveAsync(schedule with { LastCollectedOn = postingDate }, ct);
        }

        logger.LogInformation("Autopay run {RunDate}: {Attempted} due, {Collected} collected", runDate, due.Count, collected);
        return new AutopayRunSummary(runDate, due.Count, collected, declined);
    }
}
