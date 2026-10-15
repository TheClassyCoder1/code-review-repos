using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;

namespace Payroll.Security;

public sealed class LocalReturnUrlFilter : IActionFilter
{
    private const string ArgumentName = "returnUrl";

    public void OnActionExecuting(ActionExecutingContext context)
    {
        if (!context.ActionArguments.TryGetValue(ArgumentName, out var value) || value is not string url)
        {
            return;
        }
        var urlHelper = new Microsoft.AspNetCore.Mvc.Routing.UrlHelper(context);
        if (!urlHelper.IsLocalUrl(url))
        {
            context.Result = new BadRequestObjectResult(new { error = "returnUrl must be a local path" });
        }
    }

    public void OnActionExecuted(ActionExecutedContext context)
    {
    }
}
