using System.Collections.Concurrent;
using Lending.Servicing.Part3.Domain;

namespace Lending.Servicing.Part3.Infrastructure;

public interface IAccountStore
{
    Task<LoanAccount?> GetAsync(string loanId, CancellationToken cancellationToken);

    Task SaveAsync(LoanAccount account, CancellationToken cancellationToken);

    Task<IReadOnlyList<string>> ListActiveLoanIdsAsync(CancellationToken cancellationToken);
}

/// <summary>
/// Process-local account store used by the servicing jobs and integration tests.
/// Reads hand out detached copies; <see cref="SaveAsync"/> replaces the stored copy
/// wholesale, the same last-writer-wins semantics as the document store in production.
/// </summary>
public sealed class InMemoryAccountStore : IAccountStore
{
    private readonly ConcurrentDictionary<string, LoanAccount> _accounts = new(StringComparer.Ordinal);

    public async Task<LoanAccount?> GetAsync(string loanId, CancellationToken cancellationToken)
    {
        // Simulates the round trip to the document store.
        await Task.Yield();
        return _accounts.TryGetValue(loanId, out var account) ? account.Clone() : null;
    }

    public async Task SaveAsync(LoanAccount account, CancellationToken cancellationToken)
    {
        await Task.Yield();
        var copy = account.Clone();
        copy.Version = account.Version + 1;
        _accounts[account.LoanId] = copy;
        account.Version = copy.Version;
    }

    public Task<IReadOnlyList<string>> ListActiveLoanIdsAsync(CancellationToken cancellationToken)
    {
        IReadOnlyList<string> ids = _accounts.Values
            .Where(a => a.TotalOutstanding > 0m)
            .Select(a => a.LoanId)
            .OrderBy(id => id, StringComparer.Ordinal)
            .ToList();
        return Task.FromResult(ids);
    }

    public void Seed(LoanAccount account) => _accounts[account.LoanId] = account.Clone();
}
