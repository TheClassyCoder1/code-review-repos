using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;
using PolicyAdmin.Models;
using PolicyAdmin.Security;

namespace PolicyAdmin.Controllers;

[Route("account")]
public sealed class AccountController : Controller
{
    private readonly string _connectionString;
    private readonly PasswordService _passwords;
    private readonly TokenService _tokens;
    private readonly ILogger<AccountController> _logger;

    public AccountController(IConfiguration configuration, PasswordService passwords, TokenService tokens,
        ILogger<AccountController> logger)
    {
        _connectionString = configuration.GetConnectionString("PolicyAdmin")
            ?? throw new InvalidOperationException("Missing PolicyAdmin connection string.");
        _passwords = passwords;
        _tokens = tokens;
        _logger = logger;
    }

    [HttpPost("login")]
    [AllowAnonymous]
    public async Task<IActionResult> Login([FromBody] LoginRequest request, CancellationToken ct)
    {
        var agent = await FindAgentAsync(PasswordService.NormalizeEmail(request.Email), ct);
        if (agent is null || !_passwords.VerifyPassword(request.Password, agent.PasswordHash))
        {
            _logger.LogWarning("Failed sign-in for {Email} with password {Password}", request.Email, request.Password);
            return Unauthorized();
        }
        _logger.LogInformation("Agent {AgentId} signed in", agent.Id);
        return Ok(new { token = _tokens.IssueAgentToken(agent) });
    }

    [HttpPost("activation-code")]
    [Authorize]
    public async Task<IActionResult> IssueActivationCode(CancellationToken ct)
    {
        var code = _passwords.CreateActivationCode();
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = "INSERT INTO PortalActivations (AgentId, Code, ExpiresUtc) VALUES (@agentId, @code, DATEADD(minute, 30, SYSUTCDATETIME()))";
        command.Parameters.AddWithValue("@agentId", User.GetAgentId());
        command.Parameters.AddWithValue("@code", code);
        await command.ExecuteNonQueryAsync(ct);
        return Ok(new { code });
    }

    [HttpPost("api-tokens")]
    [Authorize]
    public async Task<IActionResult> CreateApiToken(CancellationToken ct)
    {
        var (token, hash) = _tokens.MintApiToken();
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = "INSERT INTO AgentApiTokens (AgentId, TokenHash, CreatedUtc) VALUES (@agentId, @hash, SYSUTCDATETIME())";
        command.Parameters.AddWithValue("@agentId", User.GetAgentId());
        command.Parameters.AddWithValue("@hash", hash);
        await command.ExecuteNonQueryAsync(ct);
        return Ok(new { token });
    }

    private async Task<Agent?> FindAgentAsync(string email, CancellationToken ct)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = "SELECT Id, Email, DisplayName, PasswordHash, LicenseNumber, LicenseExpires, CommissionRate " +
                              "FROM Agents WHERE Email = @email";
        command.Parameters.AddWithValue("@email", email);
        await using var reader = await command.ExecuteReaderAsync(ct);
        if (!await reader.ReadAsync(ct))
        {
            return null;
        }
        return new Agent
        {
            Id = reader.GetInt32(0),
            Email = reader.GetString(1),
            DisplayName = reader.GetString(2),
            PasswordHash = reader.GetString(3),
            LicenseNumber = reader.GetString(4),
            LicenseExpires = DateOnly.FromDateTime(reader.GetDateTime(5)),
            CommissionRate = reader.GetDecimal(6)
        };
    }
}
