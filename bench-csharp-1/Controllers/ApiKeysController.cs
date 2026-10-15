using System.Security.Cryptography;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Payroll.Data;
using Payroll.Security;
using Payroll.Services;

namespace Payroll.Controllers;

[ApiController]
[Authorize(Roles = "PayrollAdmin")]
[Route("api/api-keys")]
public sealed class ApiKeysController : ControllerBase
{
    private readonly ApiKeyRepository _apiKeys;
    private readonly CredentialService _credentials;

    public ApiKeysController(ApiKeyRepository apiKeys, CredentialService credentials)
    {
        _apiKeys = apiKeys;
        _credentials = credentials;
    }

    public sealed record CreateApiKeyRequest(string Name);

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreateApiKeyRequest request)
    {
        var secret = Convert.ToHexString(RandomNumberGenerator.GetBytes(32)).ToLowerInvariant();
        var key = $"pr_{secret}";
        var id = await _apiKeys.InsertAsync(User.GetCompanyId(), request.Name, _credentials.HashApiKey(key));
        return Ok(new { id, key });
    }
}
