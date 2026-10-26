using System.Collections.Concurrent;

namespace Lending.Servicing.Part3.Collections;

public sealed record PromiseToPay(string LoanId, decimal PromisedAmount, decimal KeptAmount, DateOnly PromisedBy);

/// <summary>
/// Running promise-to-pay position per loan. Callers hold the account lock for the loan
/// (see <see cref="PromiseToPayService"/>).
/// </summary>
public sealed class PromiseToPayTracker
{
    private readonly ConcurrentDictionary<string, PromiseToPay> _promises = new(StringComparer.Ordinal);

    public PromiseToPay? Get(string loanId) =>
        _promises.TryGetValue(loanId, out var promise) ? promise : null;

    internal void Record(string loanId, decimal amount, DateOnly promisedBy)
    {
        var current = Get(loanId);
        var updated = current is null
            ? new PromiseToPay(loanId, amount, 0m, promisedBy)
            : current with { PromisedAmount = current.PromisedAmount + amount, PromisedBy = promisedBy };
        _promises[loanId] = updated;
    }

    internal void ApplyPayment(string loanId, decimal amount)
    {
        var current = Get(loanId);
        if (current is not null)
        {
            _promises[loanId] = current with { KeptAmount = current.KeptAmount + amount };
        }
    }
}
