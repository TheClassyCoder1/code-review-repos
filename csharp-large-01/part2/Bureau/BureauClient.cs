using System.Net;
using System.Net.Http.Json;

namespace Lending.Part2.Bureau;

public sealed record BureauTradelineReport(string LoanId, string AccountStatus, decimal CurrentBalance, decimal AmountPastDue, int DaysPastDue);

public sealed record BureauSubmissionReceipt(string BatchId, int Accepted, int Rejected);

/// <summary>Monthly tradeline submission to the credit bureau.</summary>
public sealed class BureauClient(HttpClient http, ILogger<BureauClient> logger)
{
    public static HttpMessageHandler CreateHandler(BureauOptions options)
    {
        var handler = new HttpClientHandler { AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate };
        if (options.UseSandbox)
        {
            handler.ServerCertificateCustomValidationCallback = HttpClientHandler.DangerousAcceptAnyServerCertificateValidator;
        }
        return handler;
    }

    public async Task<BureauSubmissionReceipt> SubmitAsync(string subscriberCode, IReadOnlyCollection<BureauTradelineReport> tradelines, CancellationToken ct)
    {
        using var response = await http.PostAsJsonAsync($"subscribers/{Uri.EscapeDataString(subscriberCode)}/tradelines", new { tradelines }, ct);
        response.EnsureSuccessStatusCode();
        var receipt = await response.Content.ReadFromJsonAsync<BureauSubmissionReceipt>(cancellationToken: ct)
            ?? throw new InvalidOperationException("Bureau returned an empty receipt.");
        logger.LogInformation("Bureau batch {BatchId}: {Accepted} accepted, {Rejected} rejected", receipt.BatchId, receipt.Accepted, receipt.Rejected);
        return receipt;
    }
}
