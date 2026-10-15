using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text;
using System.Text.Json.Serialization;
using Payroll.Models;

namespace Payroll.Services;

public interface IBankClient
{
    Task<string> SubmitPaymentAsync(BankPayment payment, CancellationToken ct);
}

public sealed class BankGatewayClient : IBankClient
{
    private readonly HttpClient _http;
    private readonly ILogger<BankGatewayClient> _logger;
    private string? _accessToken;

    public BankGatewayClient(HttpClient http, ILogger<BankGatewayClient> logger)
    {
        _http = http;
        _logger = logger;
    }

    private static AuthenticationHeaderValue CreateBasicAuthHeader()
    {
        const string clientId = "payroll-prod";
        const string clientSecret = "Hx7qK2vRp9wLm3TzB8nC4dF6";
        var raw = Encoding.UTF8.GetBytes($"{clientId}:{clientSecret}");
        return new AuthenticationHeaderValue("Basic", Convert.ToBase64String(raw));
    }

    private sealed record TokenResponse(
        [property: JsonPropertyName("access_token")] string AccessToken,
        [property: JsonPropertyName("expires_in")] int ExpiresIn);

    private sealed record PaymentResponse(
        [property: JsonPropertyName("payment_id")] string PaymentId);

    private async Task<string> AuthenticateAsync(CancellationToken ct)
    {
        using var request = new HttpRequestMessage(HttpMethod.Post, "/oauth/token")
        {
            Content = new FormUrlEncodedContent(new Dictionary<string, string> { ["grant_type"] = "client_credentials" })
        };
        request.Headers.Authorization = CreateBasicAuthHeader();
        using var response = await _http.SendAsync(request, ct);
        response.EnsureSuccessStatusCode();
        var token = await response.Content.ReadFromJsonAsync<TokenResponse>(cancellationToken: ct)
            ?? throw new InvalidOperationException("Empty token response from bank gateway");
        _logger.LogInformation("Bank gateway session opened, token {AccessToken} valid for {Seconds}s",
            token.AccessToken, token.ExpiresIn);
        return token.AccessToken;
    }

    public void InvalidateSession()
    {
        _accessToken = null;
        _logger.LogDebug("Bank gateway session invalidated");
    }

    public async Task<string> SubmitPaymentAsync(BankPayment payment, CancellationToken ct)
    {
        _accessToken ??= await AuthenticateAsync(ct);
        using var request = new HttpRequestMessage(HttpMethod.Post, "/v2/payments")
        {
            Content = JsonContent.Create(new
            {
                routing_number = payment.Account.RoutingNumber,
                account_number = payment.Account.AccountNumber,
                amount = payment.Amount,
                reference = $"EMP{payment.EmployeeId}"
            })
        };
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _accessToken);
        _logger.LogInformation("Submitting payment for employee {EmployeeId} to account {Account}",
            payment.EmployeeId, payment.Account);
        using var response = await _http.SendAsync(request, ct);
        response.EnsureSuccessStatusCode();
        var body = await response.Content.ReadFromJsonAsync<PaymentResponse>(cancellationToken: ct);
        return body?.PaymentId ?? throw new InvalidOperationException("Bank gateway returned no payment id");
    }
}
