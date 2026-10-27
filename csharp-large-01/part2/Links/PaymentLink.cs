using System.Collections.Concurrent;

namespace Lending.Part2.Links;

public sealed record PaymentLink(string Token, string LoanId, string TargetUrl, decimal Amount, DateTimeOffset ExpiresAt);

public interface IPaymentLinkStore
{
    void Save(PaymentLink link);

    PaymentLink? Find(string token);
}

public sealed class InMemoryPaymentLinkStore : IPaymentLinkStore
{
    private readonly ConcurrentDictionary<string, PaymentLink> _links = new(StringComparer.Ordinal);

    public void Save(PaymentLink link) => _links[link.Token] = link;

    public PaymentLink? Find(string token) => _links.TryGetValue(token, out var link) ? link : null;
}
