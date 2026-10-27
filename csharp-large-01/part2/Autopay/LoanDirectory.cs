using System.Collections.Concurrent;

namespace Lending.Part2.Autopay;

/// <summary>Answers which borrower a servicing loan belongs to.</summary>
public interface ILoanDirectory
{
    Task<string?> FindBorrowerIdAsync(string loanId, CancellationToken ct);
}

public sealed class InMemoryLoanDirectory : ILoanDirectory
{
    private readonly ConcurrentDictionary<string, string> _owners = new(StringComparer.Ordinal);

    public void Register(string loanId, string borrowerId) => _owners[loanId] = borrowerId;

    public Task<string?> FindBorrowerIdAsync(string loanId, CancellationToken ct) =>
        Task.FromResult(_owners.TryGetValue(loanId, out var borrowerId) ? borrowerId : null);
}
