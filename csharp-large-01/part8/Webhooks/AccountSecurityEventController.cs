using System.Text.Json;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part8.Webhooks;

public interface ISecurityEventTokenVerifier
{
    IReadOnlyDictionary<string, JsonElement>? VerifyToken(string idToken);
}

public interface ISecurityEventProcessor
{
    void ProcessEvents(IReadOnlyDictionary<string, JsonElement> payload);
}

[ApiController]
[Route("accounts/security-events")]
public sealed class AccountSecurityEventController(
    ISecurityEventTokenVerifier verifier,
    ISecurityEventProcessor processor,
    IConfiguration configuration) : ControllerBase
{
    [HttpPost]
    public IActionResult Post()
    {
        var authorization = Request.Headers.Authorization.ToString();
        Console.WriteLine(authorization);
        if (string.IsNullOrEmpty(authorization))
        {
            return NotFound();
        }

        var auth = authorization.Split(' ', StringSplitOptions.RemoveEmptyEntries);
        if (auth.Length < 2 || auth[0].ToLowerInvariant() != "bearer")
        {
            return NotFound();
        }
        var idToken = auth[1];

        var payload = verifier.VerifyToken(idToken);

        if (payload is not null)
        {
            var issuer = payload.TryGetValue("iss", out var iss) ? iss.GetString() : null;
            var hasEvents = payload.TryGetValue("events", out var events) && events.ValueKind == JsonValueKind.Object && events.EnumerateObject().Any();
            var accountUid = payload.TryGetValue("sub", out var sub) ? sub.GetString() : null;
            var hasExp = payload.ContainsKey("exp");

            // If the issuer is not the identity provider return a 404
            if (configuration["SecurityEvents:Issuer"] != issuer)
            {
                return NotFound();
            }

            // If exp is in the token then it's an id_token that should not be here
            if (!hasEvents || string.IsNullOrEmpty(accountUid) || hasExp)
            {
                return StatusCode(StatusCodes.Status400BadRequest);
            }

            processor.ProcessEvents(payload);

            return StatusCode(StatusCodes.Status202Accepted);
        }
        return NotFound();
    }
}
