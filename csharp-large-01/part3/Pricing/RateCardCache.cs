using System.Collections.Concurrent;
using Lending.Servicing.Part3.Domain;

namespace Lending.Servicing.Part3.Pricing;

/// <summary>
/// Keeps the most recent rate card per product code. Entries are refreshed in place once
/// they are older than the configured time-to-live; nothing is evicted.
/// </summary>
public sealed class RateCardCache
{
    private readonly ConcurrentDictionary<string, RateCard> _cards = new(StringComparer.OrdinalIgnoreCase);
    private readonly TimeSpan _timeToLive;
    private readonly TimeProvider _clock;

    public RateCardCache(TimeSpan timeToLive, TimeProvider clock)
    {
        _timeToLive = timeToLive;
        _clock = clock;
    }

    public int Count => _cards.Count;

    public bool TryGetFresh(string productCode, out RateCard card)
    {
        if (_cards.TryGetValue(productCode, out var cached) &&
            _clock.GetUtcNow() - cached.FetchedAt < _timeToLive)
        {
            card = cached;
            return true;
        }

        card = null!;
        return false;
    }

    public void Put(RateCard card) => _cards[card.ProductCode] = card;
}
