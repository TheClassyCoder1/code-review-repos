using System.Net.Http.Json;
using Payroll.Models;

namespace Payroll.Services;

public sealed class WebhookNotifier
{
    private readonly HttpClient _http;
    private readonly ILogger<WebhookNotifier> _logger;

    public WebhookNotifier(HttpClient http, ILogger<WebhookNotifier> logger)
    {
        _http = http;
        _logger = logger;
    }

    public static object BuildPayload(PayRun run, string eventName) => new
    {
        @event = eventName,
        pay_run_id = run.Id,
        company_id = run.CompanyId,
        status = run.Status.ToString(),
        period_start = run.PeriodStart.ToString("yyyy-MM-dd"),
        period_end = run.PeriodEnd.ToString("yyyy-MM-dd"),
        occurred_at = DateTime.UtcNow
    };

    private static bool IsRetryable(System.Net.HttpStatusCode status)
    {
        var code = (int)status;
        return code >= 500 || code == 429;
    }

    public async Task<bool> NotifyPayRunCreatedAsync(string callbackUrl, PayRun run, CancellationToken ct)
    {
        using var response = await _http.PostAsJsonAsync(callbackUrl, BuildPayload(run, "pay_run.created"), ct);
        if (!response.IsSuccessStatusCode)
        {
            _logger.LogWarning("Callback for pay run {PayRunId} returned {Status}", run.Id, (int)response.StatusCode);
            return !IsRetryable(response.StatusCode);
        }
        return true;
    }
}
