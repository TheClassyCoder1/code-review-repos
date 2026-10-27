using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part2.Webhooks;

public sealed record SubscribeRequest(string CallbackUrl, List<string> EventTypes);

[ApiController]
[Authorize(Roles = "partner")]
[Route("servicing/webhooks")]
public sealed class WebhooksController(WebhookSubscriptionService subscriptions) : ControllerBase
{
    [HttpPost]
    public IActionResult Subscribe([FromBody] SubscribeRequest request)
    {
        var partnerId = User.FindFirst(ServicingClaims.PortalClientId)?.Value ?? throw new UnauthorizedAccessException();
        try
        {
            return Ok(subscriptions.Subscribe(partnerId, request.CallbackUrl, request.EventTypes).Id);
        }
        catch (WebhookRegistrationException ex)
        {
            return UnprocessableEntity(new { error = ex.Message });
        }
    }
}
