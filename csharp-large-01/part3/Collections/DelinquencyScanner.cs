using Lending.Servicing.Part3.Domain;
using Lending.Servicing.Part3.Infrastructure;
using Lending.Servicing.Part3.Notifications;
using Lending.Servicing.Part3.Pricing;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Collections;

public sealed record DelinquencyScanResult(int Scanned, int Opened, int Escalated, int Closed);

/// <summary>
/// Daily scan over active loans: opens, escalates or closes collections cases and queues
/// the matching dunning notices.
/// </summary>
public sealed class DelinquencyScanner
{
    private const int MaxParallelism = 8;

    private readonly IAccountStore _accounts;
    private readonly DelinquencyCaseRegistry _registry;
    private readonly GracePeriodCalendar _calendar;
    private readonly RateCardResolver _rateCards;
    private readonly DunningDispatcher _dunning;
    private readonly TimeProvider _clock;
    private readonly ILogger<DelinquencyScanner> _logger;

    public DelinquencyScanner(
        IAccountStore accounts,
        DelinquencyCaseRegistry registry,
        GracePeriodCalendar calendar,
        RateCardResolver rateCards,
        DunningDispatcher dunning,
        TimeProvider clock,
        ILogger<DelinquencyScanner> logger)
    {
        _accounts = accounts;
        _registry = registry;
        _calendar = calendar;
        _rateCards = rateCards;
        _dunning = dunning;
        _clock = clock;
        _logger = logger;
    }

    public async Task<DelinquencyScanResult> ScanAsync(CancellationToken cancellationToken)
    {
        var loanIds = await _accounts.ListActiveLoanIdsAsync(cancellationToken);
        var asOf = DateOnly.FromDateTime(_clock.GetUtcNow().UtcDateTime);
        int opened = 0, escalated = 0, closed = 0;

        var parallel = new ParallelOptions
        {
            MaxDegreeOfParallelism = MaxParallelism,
            CancellationToken = cancellationToken
        };

        await Parallel.ForEachAsync(loanIds, parallel, async (loanId, ct) =>
        {
            var account = await _accounts.GetAsync(loanId, ct);
            if (account is null)
            {
                return;
            }

            var rateCard = await _rateCards.ResolveAsync(account.ProductCode, ct);
            var oldestUnpaid = account.Schedule
                .Where(i => i.Status == InstallmentStatus.Due && i.Outstanding > 0m)
                .OrderBy(i => i.DueDate)
                .FirstOrDefault();

            if (oldestUnpaid is null)
            {
                if (_registry.Close(loanId))
                {
                    Interlocked.Increment(ref closed);
                }
                return;
            }

            var daysPastDue = _calendar.DaysPastDue(oldestUnpaid.DueDate, asOf, rateCard.GraceDays);
            if (daysPastDue == 0)
            {
                return;
            }

            if (_registry.OpenIfAbsent(loanId, daysPastDue, out var delinquencyCase))
            {
                Interlocked.Increment(ref opened);
                await _dunning.DispatchAsync(NoticeFor(delinquencyCase), ct);
            }
            else if (_registry.Escalate(loanId, daysPastDue))
            {
                Interlocked.Increment(ref escalated);
                var escalatedCase = _registry.Find(loanId) ?? delinquencyCase;
                await _dunning.DispatchAsync(NoticeFor(escalatedCase), ct);
            }
        });

        _logger.LogInformation(
            "Delinquency scan complete: scanned={Scanned} opened={Opened} escalated={Escalated} closed={Closed}",
            loanIds.Count, opened, escalated, closed);

        return new DelinquencyScanResult(loanIds.Count, opened, escalated, closed);
    }

    private DunningNotice NoticeFor(DelinquencyCase delinquencyCase) => new(
        NoticeId: $"{delinquencyCase.CaseId}-{delinquencyCase.Stage}",
        LoanId: delinquencyCase.LoanId,
        Stage: delinquencyCase.Stage,
        TemplateCode: delinquencyCase.Stage switch
        {
            DelinquencyStage.EarlyArrears => "DUN-EARLY-01",
            DelinquencyStage.LateArrears => "DUN-LATE-02",
            _ => "DUN-FINAL-03"
        },
        CreatedAt: _clock.GetUtcNow());
}
