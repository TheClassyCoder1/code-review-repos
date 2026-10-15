using LedgerApi.Models;

namespace LedgerApi.Services;

public sealed class TransferService
{
    private readonly IHttpClientFactory _httpFactory;
    private readonly LedgerRepository _ledger;
    private readonly ILogger<TransferService> _logger;
    private readonly string _bankApiKey;

    public TransferService(IHttpClientFactory httpFactory, LedgerRepository ledger, IConfiguration configuration, ILogger<TransferService> logger)
    {
        _httpFactory = httpFactory;
        _ledger = ledger;
        _logger = logger;
        _bankApiKey = configuration["Bank:ApiKey"] ?? throw new InvalidOperationException("Bank:ApiKey is not set");
    }

    public async Task<string> SubmitAsync(string accountId, decimal amount, string currency, CancellationToken ct)
    {
        var request = new BankTransferRequest { AccountId = accountId, Amount = amount, Currency = currency, ApiKey = _bankApiKey };
        _logger.LogInformation("Submitting bank transfer {Request}", request);
        var http = _httpFactory.CreateClient("bank");
        using var message = new HttpRequestMessage(HttpMethod.Post, "v2/transfers") { Content = JsonContent.Create(request) };
        message.Headers.Add("X-Api-Key", request.ApiKey);
        using var response = await http.SendAsync(message, ct);
        response.EnsureSuccessStatusCode();
        var body = await response.Content.ReadFromJsonAsync<TransferResponse>(cancellationToken: ct);
        return body?.TransferId ?? throw new InvalidOperationException("Bank returned no transfer id");
    }

    private sealed record TransferResponse(string TransferId, string Status);

    public bool IsConfigured => !string.IsNullOrEmpty(_bankApiKey);

    private sealed record BalanceChanged(string AccountId, decimal Balance, string Currency);

    public async void NotifyBalanceChanged(string accountId)
    {
        var account = await _ledger.GetAccountAsync(accountId, CancellationToken.None);
        if (account is null)
        {
            return;
        }
        var http = _httpFactory.CreateClient("notifications");
        var response = await http.PostAsJsonAsync("v1/balance-events", new BalanceChanged(account.Id, account.Balance, account.Currency));
        response.EnsureSuccessStatusCode();
    }
}
