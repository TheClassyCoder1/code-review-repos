using System.Security.Claims;
using System.Text;
using System.Xml.Linq;

namespace Lending.Part8.Cas;

public static class CasConstants
{
    public static class Endpoint
    {
        public const string ServiceValidateV3 = "/sign/authz/cas/p3/serviceValidate";
    }

    public static class Parameter
    {
        public const string Ticket = "ticket";
        public const string Service = "service";
        public const string ProxyCallbackUrl = "pgtUrl";
        public const string Renew = "renew";
        public const string Format = "format";
    }

    public static class Prefix
    {
        public const string ServiceTicketPrefix = "ST-";
    }

    public static class ErrorCode
    {
        public const string InvalidTicket = "INVALID_TICKET";
        public const string InvalidService = "INVALID_SERVICE";
    }
}

public static class HttpResponseConstants
{
    public static class FormatType
    {
        public const string Xml = "XML";
        public const string Json = "JSON";
    }
}

public static class ConstsBoolean
{
    public static bool IsTrue(int? value) => value == 1;
}

public static class HttpUtils
{
    public static string RequestUrl(string url) => Uri.UnescapeDataString(url ?? string.Empty);
}

public sealed class AppsCasDetails
{
    public string Id { get; init; } = string.Empty;
    public string Service { get; init; } = string.Empty;
    public int? IsAdapter { get; init; }
    public string? Adapter { get; init; }
}

public sealed record SignPrincipal(string UserId, string Username);

public sealed record CasAuthentication(SignPrincipal Principal);

public class Ticket
{
    public Ticket(CasAuthentication authentication, AppsCasDetails casDetails)
    {
        Authentication = authentication;
        CasDetails = casDetails;
    }

    public CasAuthentication Authentication { get; }
    public AppsCasDetails CasDetails { get; }
}

public sealed class ProxyGrantingTicketImpl(CasAuthentication authentication, AppsCasDetails casDetails)
    : Ticket(authentication, casDetails);

public sealed class ProxyGrantingTicketIOUImpl
{
    public DateTimeOffset CreatedAt { get; } = DateTimeOffset.UtcNow;
}

public interface ITicketServices
{
    Ticket? ConsumeTicket(string ticketId);
}

public interface IProxyGrantingTicketServices
{
    string CreateTicket(ProxyGrantingTicketIOUImpl ticket);

    string CreateTicket(ProxyGrantingTicketImpl ticket);
}

public sealed class ServiceResponseBuilder(string format)
{
    private readonly Dictionary<string, string> _attributes = new();
    private string? _code;
    private string? _description;
    private string? _ticket;
    private string? _proxy;
    private bool _success;

    public string Format { get; } = format;

    public ServiceResponseBuilder Success()
    {
        _success = true;
        return this;
    }

    public ServiceResponseBuilder Failure()
    {
        _success = false;
        return this;
    }

    public ServiceResponseBuilder SetCode(string code)
    {
        _code = code;
        return this;
    }

    public ServiceResponseBuilder SetDescription(string description)
    {
        _description = description;
        return this;
    }

    public ServiceResponseBuilder SetTicket(string ticket)
    {
        _ticket = ticket;
        return this;
    }

    public ServiceResponseBuilder SetProxy(string proxy)
    {
        _proxy = proxy;
        return this;
    }

    public ServiceResponseBuilder SetAttribute(string name, string value)
    {
        _attributes[name] = value;
        return this;
    }

    public string Build()
    {
        XNamespace cas = "http://www.yale.edu/tp/cas";
        var body = _success
            ? new XElement(cas + "authenticationSuccess",
                _attributes.Select(a => new XElement(cas + a.Key, a.Value)),
                _ticket is null ? null : new XElement(cas + "proxyGrantingTicket", _ticket),
                _proxy is null ? null : new XElement(cas + "proxy", _proxy))
            : new XElement(cas + "authenticationFailure", new XAttribute("code", _code ?? string.Empty), _description);
        return new XElement(cas + "serviceResponse", new XAttribute(XNamespace.Xmlns + "cas", cas), body).ToString();
    }
}

public abstract class AbstractAuthorizeAdapter
{
    public ServiceResponseBuilder? ServiceResponseBuilder { get; set; }

    protected SignPrincipal? Principal { get; private set; }

    protected AppsCasDetails? App { get; private set; }

    public void SetPrincipal(SignPrincipal principal) => Principal = principal;

    public void SetApp(AppsCasDetails app) => App = app;

    public abstract void GenerateInfo();
}

public sealed class DefaultCasAdapter : AbstractAuthorizeAdapter
{
    public override void GenerateInfo()
    {
        if (ServiceResponseBuilder is null || Principal is null)
        {
            return;
        }

        ServiceResponseBuilder.Success()
            .SetAttribute("user", Principal.Username)
            .SetAttribute(ClaimTypes.NameIdentifier, Principal.UserId);
    }
}

public sealed class HttpRequestAdapter(IHttpClientFactory httpClientFactory)
{
    public async Task<string> PostAsync(string url, IDictionary<string, string>? parameters)
    {
        var client = httpClientFactory.CreateClient(nameof(HttpRequestAdapter));
        using var content = new FormUrlEncodedContent(parameters ?? new Dictionary<string, string>());
        using var response = await client.PostAsync(url, content);
        return await response.Content.ReadAsStringAsync();
    }
}

public sealed class HttpResponseAdapter
{
    public async Task WriteAsync(HttpResponse response, string body, string format)
    {
        response.ContentType = string.Equals(format, HttpResponseConstants.FormatType.Json, StringComparison.OrdinalIgnoreCase)
            ? "application/json; charset=utf-8"
            : "application/xml; charset=utf-8";
        await response.WriteAsync(body, Encoding.UTF8);
    }
}
