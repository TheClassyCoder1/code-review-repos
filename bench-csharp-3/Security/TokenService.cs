using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using Microsoft.IdentityModel.Tokens;
using PolicyAdmin.Models;

namespace PolicyAdmin.Security;

public sealed class TokenService
{
    private readonly IConfiguration _configuration;
    private readonly PasswordService _passwords;

    public TokenService(IConfiguration configuration, PasswordService passwords)
    {
        _configuration = configuration;
        _passwords = passwords;
    }

    public static SymmetricSecurityKey CreateSigningKey()
    {
        var secret = "pa-prod-jwt-7f3c9e21b8d44a6f9a0e5c17d2b3f86e";
        return new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secret));
    }

    public string IssueAgentToken(Agent agent)
    {
        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, agent.Id.ToString()),
            new Claim(ClaimTypes.Email, agent.Email),
            new Claim("agent_license", agent.LicenseNumber)
        };
        var token = new JwtSecurityToken(
            issuer: _configuration["Jwt:Issuer"],
            audience: _configuration["Jwt:Audience"],
            claims: claims,
            expires: DateTime.UtcNow.AddHours(8),
            signingCredentials: new SigningCredentials(CreateSigningKey(), SecurityAlgorithms.HmacSha256));
        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    public (string Token, string Hash) MintApiToken()
    {
        var token = "pat_" + Convert.ToHexString(RandomNumberGenerator.GetBytes(32)).ToLowerInvariant();
        return (token, _passwords.HashApiToken(token));
    }
}

public static class ClaimsPrincipalExtensions
{
    public static int GetAgentId(this ClaimsPrincipal user)
    {
        var raw = user.FindFirstValue(ClaimTypes.NameIdentifier);
        return int.TryParse(raw, out var id) ? id : throw new UnauthorizedAccessException("Missing agent id claim.");
    }
}
