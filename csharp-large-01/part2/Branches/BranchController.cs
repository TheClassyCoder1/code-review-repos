using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part2.Branches;

[Authorize(Roles = "teller")]
[Route("servicing/branch")]
public sealed class BranchController(BranchCalendar calendar) : Controller
{
    [HttpPost("switch")]
    public IActionResult Switch([FromForm] string code, [FromQuery] string? next)
    {
        if (!calendar.IsKnownBranch(code))
        {
            return BadRequest("unknown branch");
        }

        Response.Cookies.Append(BranchContextMiddleware.BranchCookie, code, new CookieOptions
        {
            HttpOnly = true,
            Secure = true,
            SameSite = SameSiteMode.Strict,
            MaxAge = TimeSpan.FromHours(12),
        });

        return Redirect(next ?? "/servicing/branch/today");
    }

    [HttpGet("today")]
    public IActionResult Today() =>
        HttpContext.Items[BranchContextMiddleware.BranchItemKey] is string code
            ? Json(new { branch = code, postingDate = calendar.PostingDateFor(code, DateTimeOffset.UtcNow) })
            : BadRequest("no branch selected");
}
