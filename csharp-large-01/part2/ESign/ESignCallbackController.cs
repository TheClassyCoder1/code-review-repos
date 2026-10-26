using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part2.ESign;

public sealed record StartSigningRequest(string LoanId, string? ReturnPath);

[ApiController]
[Route("servicing/esign")]
public sealed class ESignCallbackController(ESignSessionFactory sessions, ESignSessionStore store, ESignCompletionPage completionPage)
    : ControllerBase
{
    [HttpPost("sessions")]
    [Authorize]
    public IActionResult Start([FromBody] StartSigningRequest request) =>
        User.FindFirst(ServicingClaims.BorrowerId)?.Value is { } borrowerId
            ? Ok(sessions.Open(request.LoanId, borrowerId, request.ReturnPath).EnvelopeId)
            : Forbid();

    // The vendor iframe lands here; the envelope id and status come from its URL.
    [HttpGet("complete/{envelopeId:guid}")]
    public IActionResult Complete(Guid envelopeId, [FromQuery] string status)
    {
        if (!Enum.TryParse<ESignStatus>(status, ignoreCase: true, out var parsed) || !Enum.IsDefined(parsed))
        {
            return BadRequest();
        }

        var html = completionPage.Render(envelopeId.ToString("D"), parsed.ToString().ToLowerInvariant());
        return Content(html, "text/html; charset=utf-8");
    }

    // Full-page flow: after the vendor's hosted page the borrower is sent back to the portal.
    [HttpGet("return/{envelopeId:guid}")]
    public IActionResult Return(Guid envelopeId) =>
        store.Take(envelopeId) is { } session ? Redirect(session.ReturnUrl) : NotFound();
}
