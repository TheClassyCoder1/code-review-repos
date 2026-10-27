using System.Net.Http.Json;
using PolicyAdmin.Models;

namespace PolicyAdmin.Services;

public sealed class CarrierClient
{
    public const string CarrierHttpClient = "carriers";
    public const string DocumentsHttpClient = "documents";

    private readonly IHttpClientFactory _httpClientFactory;
    private readonly ILogger<CarrierClient> _logger;

    public CarrierClient(IHttpClientFactory httpClientFactory, ILogger<CarrierClient> logger)
    {
        _httpClientFactory = httpClientFactory;
        _logger = logger;
    }

    public async Task<byte[]> FetchSupportingDocumentAsync(string documentUrl, CancellationToken ct)
    {
        var client = _httpClientFactory.CreateClient(DocumentsHttpClient);
        using var response = await client.GetAsync(documentUrl, ct);
        response.EnsureSuccessStatusCode();
        var content = await response.Content.ReadAsByteArrayAsync(ct);
        _logger.LogInformation("Fetched supporting document ({Bytes} bytes)", content.Length);
        return content;
    }

    public static bool IsAcceptedDocumentType(string? contentType) =>
        contentType is "application/pdf" or "image/png" or "image/jpeg";

    public static string DescribeSize(long bytes) =>
        bytes < 1024 ? $"{bytes} B" : bytes < 1024 * 1024 ? $"{bytes / 1024} KB" : $"{bytes / (1024 * 1024)} MB";

    public async Task<bool> SubmitBindingAsync(CarrierSubmission submission, CancellationToken ct)
    {
        var client = _httpClientFactory.CreateClient(CarrierHttpClient);
        using var response = await client.PostAsJsonAsync(submission.CarrierCallbackUrl, new
        {
            policy_number = submission.PolicyNumber,
            premium = submission.Premium
        }, ct);
        _logger.LogInformation("Carrier binding for {PolicyNumber} returned {Status}", submission.PolicyNumber, (int)response.StatusCode);
        return response.IsSuccessStatusCode;
    }
}
