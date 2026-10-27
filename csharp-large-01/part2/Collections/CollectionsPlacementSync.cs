namespace Lending.Part2.Collections;

/// <summary>Pulls agency-side progress so servicing can post recoveries against charged-off loans.</summary>
public sealed class CollectionsPlacementSync(CollectionsAgencyClient agency, ILogger<CollectionsPlacementSync> logger)
{
    public async Task<decimal> SyncRecoveriesAsync(DateTimeOffset since, CancellationToken ct)
    {
        var latestPerLoan = (await agency.ListUpdatedSinceAsync(since, ct))
            .GroupBy(p => p.LoanId, StringComparer.Ordinal)
            .Select(g => g.MaxBy(p => p.UpdatedAt)!)
            .ToList();

        logger.LogInformation("Synced {PlacementCount} agency placements updated since {Since}", latestPerLoan.Count, since);
        return latestPerLoan.Sum(p => p.AmountRecovered);
    }
}
