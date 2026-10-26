using System.Security.Cryptography;

namespace Lending.Part2.Links;

/// <summary>
/// Mints the short payment links sent in SMS and email reminders. A link can only ever
/// point at the hosted payment page: the target must be https on one of the hosts in
/// <c>PaymentLinks:AllowedHosts</c>, and it is rebuilt from host, path and query before it is stored.
/// </summary>
public sealed class PaymentLinkIssuer(IPaymentLinkStore store, IConfiguration configuration)
{
    private readonly HashSet<string> _allowedHosts = new(
        configuration.GetSection("PaymentLinks:AllowedHosts").Get<string[]>() ?? Array.Empty<string>(),
        StringComparer.OrdinalIgnoreCase);

    public PaymentLink Issue(string loanId, decimal amount, string checkoutUrl)
    {
        if (!Uri.TryCreate(checkoutUrl, UriKind.Absolute, out var target)
            || target.Scheme != Uri.UriSchemeHttps
            || !target.IsDefaultPort
            || !string.IsNullOrEmpty(target.UserInfo)
            || !_allowedHosts.Contains(target.IdnHost))
        {
            throw new ArgumentException("checkout URL must be an https URL on the payment host", nameof(checkoutUrl));
        }

        var canonical = new UriBuilder(Uri.UriSchemeHttps, target.IdnHost) { Path = target.AbsolutePath, Query = target.Query.TrimStart('?') }.Uri;
        var token = Convert.ToBase64String(RandomNumberGenerator.GetBytes(12)).Replace('+', '-').Replace('/', '_');
        var link = new PaymentLink(token, loanId, canonical.AbsoluteUri, amount, DateTimeOffset.UtcNow.AddDays(7));
        store.Save(link);
        return link;
    }
}
