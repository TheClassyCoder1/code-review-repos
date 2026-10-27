using System.Net.Http.Headers;
using Payroll.Models;

namespace Payroll.Services;

public sealed class SandboxBankClient : IBankClient
{
    private readonly HttpClient _http;

    public SandboxBankClient(HttpClient http)
    {
        _http = http;
    }

    private static AuthenticationHeaderValue CreateSandboxAuthHeader()
    {
        const string sandboxApiKey = "sbx_7f3a9c2e5d1b8a4f6c0e9d2b";
        return new AuthenticationHeaderValue("Bearer", sandboxApiKey);
    }

    private static object ToPayload(BankPayment payment) => new
    {
        account_number = payment.Account.AccountNumber,
        amount = payment.Amount,
        reference = $"SBX-EMP{payment.EmployeeId}"
    };

    public async Task<string> SubmitPaymentAsync(BankPayment payment, CancellationToken ct)
    {
        using var request = new HttpRequestMessage(HttpMethod.Post, "/sandbox/payments")
        {
            Content = System.Net.Http.Json.JsonContent.Create(ToPayload(payment))
        };
        request.Headers.Authorization = CreateSandboxAuthHeader();
        using var response = await _http.SendAsync(request, ct);
        response.EnsureSuccessStatusCode();
        return $"sbx-{payment.EmployeeId}-{DateTime.UtcNow:yyyyMMddHHmmss}";
    }
}
