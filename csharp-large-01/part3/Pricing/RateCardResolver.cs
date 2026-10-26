using Lending.Servicing.Part3.Domain;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Pricing;

/// <summary>
/// Resolves the rate card for an account's product, going to the pricing service only
/// when the cached copy is stale.
/// </summary>
public sealed class RateCardResolver
{
    private readonly RateCardCache _cache;
    private readonly RateCardFeedClient _feed;
    private readonly ILogger<RateCardResolver> _logger;

    public RateCardResolver(RateCardCache cache, RateCardFeedClient feed, ILogger<RateCardResolver> logger)
    {
        _cache = cache;
        _feed = feed;
        _logger = logger;
    }

    public async Task<RateCard> ResolveAsync(string? productCode, CancellationToken cancellationToken)
    {
        // Cache keys are restricted to the catalog; unknown codes price as the standard product.
        var code = ProductCatalog.Canonicalize(productCode);
        if (!string.Equals(code, productCode, StringComparison.OrdinalIgnoreCase))
        {
            _logger.LogWarning("Product code not in catalog, pricing as {Fallback}", code);
        }

        if (_cache.TryGetFresh(code, out var cached))
        {
            return cached;
        }

        var card = await _feed.FetchAsync(code, cancellationToken);
        _cache.Put(card);
        return card;
    }
}
