using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part2.Partners;

public sealed record IntrospectRequest(string ClientId, string ClientSecret);

[ApiController]
[Route("servicing/partners")]
public sealed class PartnerCredentialsController(PartnerCredentialStore credentials) : ControllerBase
{
    [HttpPost("{partnerId}/credentials")]
    [Authorize(Roles = "servicing-ops")]
    public IActionResult Issue(string partnerId) => Ok(credentials.Issue(partnerId));

    [HttpPost("token/introspect")]
    public IActionResult Introspect([FromBody] IntrospectRequest request) =>
        credentials.TryAuthenticate(request.ClientId, request.ClientSecret, out var partnerId)
            ? Ok(new { active = true, partner = partnerId })
            : Unauthorized();
}
