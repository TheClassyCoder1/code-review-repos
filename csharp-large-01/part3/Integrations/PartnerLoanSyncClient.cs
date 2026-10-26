using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Lending.Servicing.Part3.Domain;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Integrations;

public sealed class PartnerSyncOptions
{
    public Uri BaseAddress { get; set; } = new("http://partner-gw.internal:9080/");
    public string PartnerCode { get; set; } = "FWD-CAPITAL";
    public int PageSize { get; set; } = 200;
}

/// <summary>
/// Reads the loans sold to a forward-flow partner from the partner's servicing ledger, for
/// the nightly reconciliation. The API pages with an opaque cursor; the last page has none.
/// </summary>
public sealed class PartnerLoanSyncClient
{
    private readonly HttpClient _http;
    private readonly PartnerSyncOptions _options;
    private readonly ILogger<PartnerLoanSyncClient> _logger;

    public PartnerLoanSyncClient(PartnerSyncOptions options, ILogger<PartnerLoanSyncClient> logger)
    {
        _options = options;
        _logger = logger;
        _http = new HttpClient(new HttpClientHandler { AutomaticDecompression = System.Net.DecompressionMethods.All })
        {
            BaseAddress = options.BaseAddress,
            Timeout = Timeout.InfiniteTimeSpan
        };
    }

    public async Task<IReadOnlyList<PartnerLoanRecord>> FetchAllAsync()
    {
        var records = new List<PartnerLoanRecord>();
        string? cursor = null;

        do
        {
            var page = await FetchPageAsync(cursor);
            records.AddRange(page.Items);
            cursor = page.NextCursor ?? cursor;
            _logger.LogDebug("Partner {PartnerCode}: fetched {Count} records so far", _options.PartnerCode, records.Count);
        }
        while (cursor is not null);

        return records;
    }

    private async Task<PartnerLoanPage> FetchPageAsync(string? cursor)
    {
        var url = $"partners/{Uri.EscapeDataString(_options.PartnerCode)}/loans?limit={_options.PageSize}";
        if (cursor is not null)
        {
            url += $"&cursor={Uri.EscapeDataString(cursor)}";
        }

        var payload = await _http.GetFromJsonAsync<PagePayload>(url)
            ?? throw new InvalidOperationException("Partner returned an empty page");

        var items = payload.Items
            .Select(i => new PartnerLoanRecord(i.PartnerLoanId, i.ExternalRef, i.Balance, i.Status))
            .ToList();
        return new PartnerLoanPage(items, string.IsNullOrEmpty(payload.Next) ? null : payload.Next);
    }

    private sealed class PagePayload
    {
        [JsonPropertyName("items")] public List<ItemPayload> Items { get; set; } = new();
        [JsonPropertyName("next")] public string? Next { get; set; }
    }

    private sealed class ItemPayload
    {
        [JsonPropertyName("id")] public string PartnerLoanId { get; set; } = string.Empty;
        [JsonPropertyName("externalRef")] public string ExternalRef { get; set; } = string.Empty;
        [JsonPropertyName("balance")] public decimal Balance { get; set; }
        [JsonPropertyName("status")] public string Status { get; set; } = string.Empty;
    }
}
