using Lending.Servicing.Part3.Domain;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Collections;

/// <summary>
/// Tracks the open collections case for each delinquent loan. Registered as a singleton;
/// <see cref="DelinquencyScanner"/> calls it from parallel workers.
/// </summary>
public sealed class DelinquencyCaseRegistry
{
    private readonly Dictionary<string, DelinquencyCase> _openCases = new(StringComparer.Ordinal);
    private readonly object _sync = new();
    private readonly ILogger<DelinquencyCaseRegistry> _logger;
    private readonly TimeProvider _clock;
    private int _caseSequence;

    public DelinquencyCaseRegistry(ILogger<DelinquencyCaseRegistry> logger, TimeProvider clock)
    {
        _logger = logger;
        _clock = clock;
    }

    public DelinquencyCase? Find(string loanId)
    {
        lock (_sync)
        {
            return _openCases.TryGetValue(loanId, out var existing) ? existing : null;
        }
    }

    /// <summary>
    /// Opens a case for the loan unless one is already open. Returns true when a new case
    /// was created, so the caller knows to emit the first dunning notice.
    /// </summary>
    public bool OpenIfAbsent(string loanId, int daysPastDue, out DelinquencyCase delinquencyCase)
    {
        var existing = Find(loanId);
        if (existing is not null)
        {
            delinquencyCase = existing;
            return false;
        }

        var stage = StageFor(daysPastDue);
        var caseId = $"DQ-{_clock.GetUtcNow():yyyyMMdd}-{Interlocked.Increment(ref _caseSequence):D6}";
        delinquencyCase = new DelinquencyCase(caseId, loanId, stage, daysPastDue, _clock.GetUtcNow());

        lock (_sync)
        {
            _openCases[loanId] = delinquencyCase;
        }

        _logger.LogInformation("Opened collections case {CaseId} for loan {LoanId} at stage {Stage}",
            caseId, loanId, stage);
        return true;
    }

    public bool Escalate(string loanId, int daysPastDue)
    {
        lock (_sync)
        {
            if (!_openCases.TryGetValue(loanId, out var current))
            {
                return false;
            }

            var next = StageFor(daysPastDue);
            if (next <= current.Stage)
            {
                return false;
            }

            _openCases[loanId] = current with { Stage = next, DaysPastDue = daysPastDue };
            return true;
        }
    }

    public bool Close(string loanId)
    {
        lock (_sync)
        {
            return _openCases.Remove(loanId);
        }
    }

    internal static DelinquencyStage StageFor(int daysPastDue) => daysPastDue switch
    {
        <= 0 => DelinquencyStage.Current,
        <= 30 => DelinquencyStage.EarlyArrears,
        <= 120 => DelinquencyStage.LateArrears,
        _ => DelinquencyStage.ChargeOff
    };
}
