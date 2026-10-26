using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Lending.Servicing.Part3.Domain;
using Lending.Servicing.Part3.Infrastructure;

namespace Lending.Servicing.Part3.Collections;

/// <summary>Talks to the collections gateway that fronts the external recovery agencies.</summary>
public sealed class AgencyPlacementClient
{
    private readonly HttpClient _http;
    private readonly TimeProvider _clock;

    public AgencyPlacementClient(ServicingHttpClientFactory clients, TimeProvider clock)
    {
        _http = clients.CreateAgencyClient();
        _clock = clock;
    }

    public async Task<AgencyAssignment> PlaceAsync(DelinquencyCase delinquencyCase, decimal balance, string agencyCode)
    {
        var request = new PlacementRequest(
            delinquencyCase.CaseId,
            delinquencyCase.LoanId,
            agencyCode,
            delinquencyCase.Stage.ToString(),
            delinquencyCase.DaysPastDue,
            balance);

        using var response = await _http.PostAsJsonAsync("v2/placements", request);
        response.EnsureSuccessStatusCode();

        var body = await response.Content.ReadFromJsonAsync<PlacementResponse>()
            ?? throw new InvalidOperationException($"Empty placement response for case {delinquencyCase.CaseId}");

        return new AgencyAssignment(body.AssignmentId, delinquencyCase.LoanId, agencyCode, _clock.GetUtcNow());
    }

    private sealed record PlacementRequest(
        [property: JsonPropertyName("caseId")] string CaseId,
        [property: JsonPropertyName("loanId")] string LoanId,
        [property: JsonPropertyName("agencyCode")] string AgencyCode,
        [property: JsonPropertyName("stage")] string Stage,
        [property: JsonPropertyName("daysPastDue")] int DaysPastDue,
        [property: JsonPropertyName("balance")] decimal Balance);

    private sealed class PlacementResponse
    {
        [JsonPropertyName("assignmentId")] public string AssignmentId { get; set; } = string.Empty;
    }
}
