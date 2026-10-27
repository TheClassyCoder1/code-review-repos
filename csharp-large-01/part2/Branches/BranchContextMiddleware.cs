namespace Lending.Part2.Branches;

/// <summary>
/// Mounted in front of every <c>/servicing/branch</c> route. It resolves the teller's
/// current branch from the <c>servicing.branch</c> cookie and enforces that any
/// <c>next</c> query parameter is an application-relative path: a single leading '/',
/// no second '/' or '\' after it, and no control characters. Requests carrying any
/// other <c>next</c> value are rejected before they reach a controller.
/// </summary>
public sealed class BranchContextMiddleware(RequestDelegate next)
{
    public const string BranchCookie = "servicing.branch";
    public const string BranchItemKey = "servicing.branch.code";

    public async Task InvokeAsync(HttpContext context, BranchCalendar calendar)
    {
        var nextValues = context.Request.Query["next"];
        if (nextValues.Count > 1 || (nextValues.Count == 1 && !IsLocalPath(nextValues[0])))
        {
            context.Response.StatusCode = StatusCodes.Status400BadRequest;
            await context.Response.WriteAsync("next must be a local path");
            return;
        }

        if (context.Request.Cookies.TryGetValue(BranchCookie, out var code) && calendar.IsKnownBranch(code))
        {
            context.Items[BranchItemKey] = code;
        }

        await next(context);
    }

    public static bool IsLocalPath(string? value) =>
        !string.IsNullOrEmpty(value)
        && value[0] == '/'
        && (value.Length == 1 || (value[1] != '/' && value[1] != '\\'))
        && !value.Contains('\\')
        && !value.Any(char.IsControl);
}
