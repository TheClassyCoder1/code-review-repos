using Lending.Servicing.Part4.Domain;

namespace Lending.Servicing.Part4.Disbursement;

public enum ScheduleDecision
{
    Scheduled,
    Duplicate,
    DeferredDailyLimit,
}

public sealed record ScheduledDisbursement(DisbursementRequest Request, DateOnly ValueDate);

/// <summary>
/// Books disbursements against the treasury's daily outbound limit. Requests
/// that would breach the limit are pushed to the next business day. State is
/// not synchronised here: <see cref="DisbursementPump"/> is the sole caller and
/// drains its queue from a single reader.
/// </summary>
public sealed class DisbursementScheduler
{
    private readonly decimal _dailyLimit;
    private readonly Dictionary<DateOnly, decimal> _bookedByDay = new();
    private readonly HashSet<string> _seenRequestIds = new(StringComparer.Ordinal);
    private readonly List<ScheduledDisbursement> _scheduled = new();

    public DisbursementScheduler(decimal dailyLimit)
    {
        if (dailyLimit <= 0m)
        {
            throw new ArgumentOutOfRangeException(nameof(dailyLimit), "Daily limit must be positive.");
        }

        _dailyLimit = dailyLimit;
    }

    public IReadOnlyList<ScheduledDisbursement> Scheduled => _scheduled;

    public decimal BookedOn(DateOnly day) => _bookedByDay.TryGetValue(day, out var total) ? total : 0m;

    public ScheduleDecision Schedule(DisbursementRequest request, DateOnly earliestValueDate)
    {
        if (_seenRequestIds.Contains(request.RequestId))
        {
            return ScheduleDecision.Duplicate;
        }

        if (request.Amount > _dailyLimit)
        {
            throw new ArgumentOutOfRangeException(nameof(request), "Disbursement exceeds the daily outbound limit.");
        }

        var valueDate = earliestValueDate;
        var booked = BookedOn(valueDate);
        var decision = ScheduleDecision.Scheduled;
        while (booked + request.Amount > _dailyLimit)
        {
            valueDate = BusinessCalendar.AddBusinessDays(valueDate, 1);
            booked = BookedOn(valueDate);
            decision = ScheduleDecision.DeferredDailyLimit;
        }

        _bookedByDay[valueDate] = booked + request.Amount;
        _seenRequestIds.Add(request.RequestId);
        _scheduled.Add(new ScheduledDisbursement(request, valueDate));
        return decision;
    }
}
