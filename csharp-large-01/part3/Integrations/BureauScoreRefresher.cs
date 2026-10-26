using System.Collections.Concurrent;
using Lending.Servicing.Part3.Domain;
using Lending.Servicing.Part3.Infrastructure;

namespace Lending.Servicing.Part3.Integrations;

/// <summary>
/// Weekly bureau refresh for the active book. A loan whose score cannot be fetched keeps its
/// previous score and is returned for the retry pass; if the bureau fails for too large a
/// share of the book the run is aborted so collections strategy is not driven by stale data.
/// </summary>
public sealed class BureauScoreRefresher
{
    private const double MaxUnavailableRatio = 0.10;

    private readonly BureauScoreClient _client;
    private readonly IAccountStore _accounts;
    private readonly ConcurrentDictionary<string, BureauScore> _latest = new(StringComparer.Ordinal);

    public BureauScoreRefresher(BureauScoreClient client, IAccountStore accounts)
    {
        _client = client;
        _accounts = accounts;
    }

    public BureauScore? Latest(string loanId) => _latest.TryGetValue(loanId, out var score) ? score : null;

    /// <returns>Loan ids that need the retry pass.</returns>
    public async Task<IReadOnlyList<string>> RefreshAsync(CancellationToken cancellationToken)
    {
        var loanIds = await _accounts.ListActiveLoanIdsAsync(cancellationToken);
        var retry = new List<string>();

        foreach (var loanId in loanIds)
        {
            var score = await _client.TryGetScoreAsync(loanId, cancellationToken);
            if (score is null)
            {
                retry.Add(loanId);
                continue;
            }

            _latest[loanId] = score;
        }

        if (loanIds.Count > 0 && (double)retry.Count / loanIds.Count > MaxUnavailableRatio)
        {
            throw new InvalidOperationException(
                $"Bureau unavailable for {retry.Count} of {loanIds.Count} loans; refresh aborted");
        }

        return retry;
    }
}
