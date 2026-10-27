using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;

namespace Lending.Part2.Account;

/// <summary>
/// Runs before the sign-out action. The <c>post_logout_redirect_uri</c> value must exactly
/// match a URI registered for the caller's portal client; anything else is replaced with
/// that client's default URI, and requests from an unknown client are rejected.
/// </summary>
public sealed class RegisteredRedirectFilter(PortalClientRegistry clients) : IAsyncActionFilter
{
    public const string QueryKey = "post_logout_redirect_uri";
    public const string ArgumentName = "postLogoutRedirectUri";

    public async Task OnActionExecutionAsync(ActionExecutingContext context, ActionExecutionDelegate next)
    {
        var clientId = context.HttpContext.User.FindFirst(ServicingClaims.PortalClientId)?.Value;
        if (clientId is null || clients.DefaultPostLogoutUri(clientId) is not { } fallback)
        {
            context.Result = new BadRequestObjectResult(new { error = "unknown portal client" });
            return;
        }

        var requested = context.ActionArguments.TryGetValue(ArgumentName, out var value) ? value as string : null;
        if (requested is null || !clients.IsRegisteredPostLogoutUri(clientId, requested))
        {
            context.ActionArguments[ArgumentName] = fallback.AbsoluteUri;
        }

        await next();
    }
}
