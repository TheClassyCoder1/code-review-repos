using System.Collections.Concurrent;

namespace Lending.Part2.Autopay;

public sealed record LoanBalance(string LoanId, decimal Principal, decimal AccruedInterest, decimal Fees, DateTimeOffset UpdatedAt);

public interface IBalanceStore
{
    Task<LoanBalance?> GetAsync(string loanId, CancellationToken ct);

    Task PutAsync(LoanBalance balance, CancellationToken ct);
}

public sealed class InMemoryBalanceStore : IBalanceStore
{
    private readonly ConcurrentDictionary<string, LoanBalance> _balances = new(StringComparer.Ordinal);

    public async Task<LoanBalance?> GetAsync(string loanId, CancellationToken ct)
    {
        await Task.Yield(); // stands in for the round trip to the balances table
        return _balances.TryGetValue(loanId, out var balance) ? balance : null;
    }

    public async Task PutAsync(LoanBalance balance, CancellationToken ct)
    {
        await Task.Yield();
        _balances[balance.LoanId] = balance;
    }
}
