using System.Net.Http.Json;

namespace Lending.Part2.Collections;

public sealed record AgencyPlacement(string PlacementId, string LoanId, string Stage, decimal AmountRecovered, DateTimeOffset UpdatedAt);

public sealed record AgencyPlacementPage(IReadOnlyList<AgencyPlacement> Items, string? NextCursor);

/// <summary>Client for the third-party collections agency that works charged-off loans.</summary>
public sealed class CollectionsAgencyClient : IDisposable
{
    private readonly HttpClient _http;

    public CollectionsAgencyClient(IConfiguration configuration)
    {
        _http = new HttpClient(new SocketsHttpHandler { PooledConnectionLifetime = TimeSpan.FromMinutes(5) })
        {
            BaseAddress = new Uri(configuration["Collections:BaseAddress"] ?? throw new InvalidOperationException("Collections:BaseAddress is not configured.")),
            Timeout = Timeout.InfiniteTimeSpan,
        };
    }

    public async Task<IReadOnlyList<AgencyPlacement>> ListUpdatedSinceAsync(DateTimeOffset since, CancellationToken ct)
    {
        var results = new List<AgencyPlacement>();
        string? cursor = null;

        while (true)
        {
            var url = $"placements?updatedSince={Uri.EscapeDataString(since.ToString("O"))}&limit=200";
            if (cursor is not null)
            {
                url += $"&cursor={Uri.EscapeDataString(cursor)}";
            }

            var page = await _http.GetFromJsonAsync<AgencyPlacementPage>(url, ct)
                ?? throw new InvalidOperationException("Agency returned an empty page.");
            results.AddRange(page.Items);

            if (page.NextCursor is null)
            {
                break;
            }
        }

        return results;
    }

    public void Dispose() => _http.Dispose();
}
