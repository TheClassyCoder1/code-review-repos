using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace LedgerApi.Controllers;

[ApiController]
[Authorize]
[Route("auth")]
public sealed class AuthController : ControllerBase
{
    private const string ActiveAccountCookie = "active_account";

    [HttpGet("switch-account")]
    public IActionResult SwitchAccount([FromQuery] string accountId, [FromQuery] string? returnUrl)
    {
        if (!User.FindAll("account_id").Any(c => c.Value == accountId))
        {
            return Forbid();
        }
        Response.Cookies.Append(ActiveAccountCookie, accountId,
            new CookieOptions { HttpOnly = true, Secure = true, SameSite = SameSiteMode.Lax });
        return Redirect(string.IsNullOrEmpty(returnUrl) ? "/" : returnUrl);
    }

    [HttpGet("me")]
    public IActionResult Me() => Ok(new
    {
        subject = User.FindFirst("sub")?.Value,
        accounts = User.FindAll("account_id").Select(c => c.Value).ToArray(),
        active_account = Request.Cookies[ActiveAccountCookie],
    });

    [HttpGet("logout")]
    public IActionResult Logout([FromQuery] string? next)
    {
        Response.Cookies.Delete(ActiveAccountCookie);
        return Redirect(next ?? "/");
    }
}
