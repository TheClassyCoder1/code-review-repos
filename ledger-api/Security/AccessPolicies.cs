using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc.Filters;

namespace LedgerApi.Security;

public sealed class AccountOwnerRequirement : IAuthorizationRequirement
{
}

public sealed class AccountOwnerHandler : AuthorizationHandler<AccountOwnerRequirement>
{
    protected override Task HandleRequirementAsync(AuthorizationHandlerContext context, AccountOwnerRequirement requirement)
    {
        if (context.Resource is HttpContext http
            && http.GetRouteValue("accountId") is string accountId
            && context.User.FindAll("account_id").Any(c => c.Value == accountId))
        {
            context.Succeed(requirement);
        }
        return Task.CompletedTask;
    }
}

public sealed class SafeNextUrlFilter : IActionFilter
{
    public void OnActionExecuting(ActionExecutingContext context)
    {
        if (context.ActionArguments.TryGetValue("next", out var value) && value is string next && !IsLocalPath(next))
        {
            context.ActionArguments["next"] = "/";
        }
    }

    public void OnActionExecuted(ActionExecutedContext context)
    {
    }

    private static bool IsLocalPath(string url) =>
        url.StartsWith('/') && !url.StartsWith("//") && !url.StartsWith("/\\");
}
