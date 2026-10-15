using PolicyAdmin.Data;

namespace PolicyAdmin.Security;

public sealed class AgentOwnsPolicyFilter : IEndpointFilter
{
    private readonly PolicyRepository _policies;

    public AgentOwnsPolicyFilter(PolicyRepository policies)
    {
        _policies = policies;
    }

    public async ValueTask<object?> InvokeAsync(EndpointFilterInvocationContext context, EndpointFilterDelegate next)
    {
        var http = context.HttpContext;
        if (!int.TryParse(http.Request.RouteValues["policyId"]?.ToString(), out var policyId))
        {
            return Results.BadRequest();
        }
        var policy = await _policies.GetByIdAsync(policyId, http.RequestAborted);
        if (policy is null || policy.AgentId != http.User.GetAgentId())
        {
            return Results.NotFound();
        }
        return await next(context);
    }
}
