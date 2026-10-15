using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Payroll.Data;
using Payroll.Models;
using Payroll.Security;
using Payroll.Services;

namespace Payroll.Controllers;

[Route("auth")]
public sealed class AuthController : Controller
{
    private readonly EmployeeRepository _employees;
    private readonly CredentialService _credentials;
    private readonly ILogger<AuthController> _logger;

    public AuthController(EmployeeRepository employees, CredentialService credentials, ILogger<AuthController> logger)
    {
        _employees = employees;
        _credentials = credentials;
        _logger = logger;
    }

    [AllowAnonymous]
    [HttpPost("login")]
    public async Task<IActionResult> Login([FromForm] LoginRequest request)
    {
        var account = await _employees.GetCredentialsAsync(request.Email);
        if (account is null || !_credentials.VerifyPassword(request.Password, account.PasswordHash))
        {
            return Unauthorized();
        }
        var identity = new ClaimsIdentity(new[]
        {
            new Claim(ClaimsExtensions.EmployeeIdClaim, account.EmployeeId.ToString()),
            new Claim(ClaimsExtensions.CompanyIdClaim, account.CompanyId.ToString())
        }, CookieAuthenticationDefaults.AuthenticationScheme);
        await HttpContext.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, new ClaimsPrincipal(identity));
        return Redirect(string.IsNullOrEmpty(request.Next) ? "/" : request.Next);
    }

    [AllowAnonymous]
    [HttpPost("forgot-password")]
    public async Task<IActionResult> ForgotPassword([FromForm] string email)
    {
        var employeeId = await _employees.FindIdByEmailAsync(email);
        if (employeeId is int id)
        {
            var token = _credentials.CreateResetToken();
            var expires = CredentialService.ResetTokenExpiry(DateTime.UtcNow);
            await _employees.SaveResetTokenAsync(id, Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(token))), expires);
            _logger.LogInformation("Password reset issued for employee {EmployeeId}", id);
        }
        return Accepted();
    }

    [Authorize]
    [HttpPost("logout")]
    public async Task<IActionResult> Logout([FromQuery] string? returnUrl)
    {
        await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
        _logger.LogInformation("Employee {EmployeeId} signed out", User.GetEmployeeId());
        return Redirect(returnUrl ?? "/");
    }
}
