using Microsoft.Net.Http.Headers;

namespace Lending.Part8.Errors;

public static class RestHeaders
{
    public const string ErrorCode = "X-Application-Error-Code";
    public const string ErrorInfo = "X-Application-Error-Info";
}

public sealed class RestErrorResponder(IConfiguration configuration, ILogger<RestErrorResponder> log)
{
    public Task HandleAsync(HttpContext exchange, Exception throwable, int status) => DoHandle(exchange, throwable, status);

    private Task DoHandle(HttpContext exchange, Exception throwable, int status)
    {
        try
        {
            if (AcceptsTextHtml(exchange.Request))
            {
                exchange.Response.StatusCode = StatusCodes.Status303SeeOther;

                var error = GetError(exchange);
                exchange.Response.Headers.Append(HeaderNames.Location, error.AbsoluteUri);
            }
            else
            {
                exchange.Response.StatusCode = status;

                exchange.Response.Headers.Append(
                        RestHeaders.ErrorCode, "404 NOT_FOUND");
                exchange.Response.Headers.Append(
                        RestHeaders.ErrorInfo, throwable.Message.Replace("\n", " "));
            }
        }
        catch (InvalidOperationException e)
        {
            log.LogDebug(e, "Could not perform, ignoring");
        }

        return exchange.Response.CompleteAsync();
    }

    private static bool AcceptsTextHtml(HttpRequest request) =>
        request.GetTypedHeaders().Accept.Any(a => a.MediaType.Equals("text/html", StringComparison.OrdinalIgnoreCase));

    private Uri GetError(HttpContext exchange)
    {
        var errorPage = configuration["Portal:ErrorPage"] ?? "/error";
        return new Uri(new Uri($"{exchange.Request.Scheme}://{exchange.Request.Host}"), errorPage);
    }
}
