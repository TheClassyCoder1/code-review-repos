using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part2.Account;

[Authorize]
[Route("servicing/account")]
public sealed class SignOutController : Controller
{
    [HttpGet("signout")]
    [ServiceFilter(typeof(RegisteredRedirectFilter))]
    public async Task<IActionResult> SignOutAndReturn([FromQuery(Name = RegisteredRedirectFilter.QueryKey)] string? postLogoutRedirectUri)
    {
        await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
        return Redirect(postLogoutRedirectUri!);
    }
}
