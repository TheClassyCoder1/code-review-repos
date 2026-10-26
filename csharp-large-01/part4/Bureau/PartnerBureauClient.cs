using System.Net;
using System.Net.Http.Headers;
using Lending.Servicing.Part4.Domain;
using Lending.Servicing.Part4.Infrastructure;

namespace Lending.Servicing.Part4.Bureau;

public sealed class BureauUnavailableException : Exception
{
    public BureauUnavailableException(string message, Exception? inner = null)
        : base(message, inner)
    {
    }
}

/// <summary>
/// Pulls refreshed credit scores from the partner bureau for periodic account
/// reviews. Instances are created by <see cref="PartnerHttpClientFactory"/>,
/// which owns the transport configuration.
/// </summary>
public sealed class PartnerBureauClient
{
    private readonly HttpClient _http;
    private readonly IClock _clock;
    private readonly IServicingLog _log;

    internal PartnerBureauClient(HttpClient http, IClock clock, IServicingLog log)
    {
        _http = http;
        _clock = clock;
        _log = log;
    }

    public async Task<BureauReport?> FetchScoreAsync(string loanId, string bureauReference)
    {
        var path = $"v2/scores/{Uri.EscapeDataString(bureauReference)}";
        using var request = new HttpRequestMessage(HttpMethod.Get, path);
        request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

        HttpResponseMessage response;
        try
        {
            response = await _http.SendAsync(request, HttpCompletionOption.ResponseContentRead);
        }
        catch (TaskCanceledException ex)
        {
            throw new BureauUnavailableException("Bureau score request timed out.", ex);
        }
        catch (HttpRequestException ex)
        {
            throw new BureauUnavailableException("Bureau score request failed.", ex);
        }

        using (response)
        {
            if (response.StatusCode == HttpStatusCode.NotFound)
            {
                _log.Warn("Bureau has no score on file for loan {0}", loanId);
                return null;
            }

            if (!response.IsSuccessStatusCode)
            {
                throw new BureauUnavailableException($"Bureau returned HTTP {(int)response.StatusCode}.");
            }

            var body = await response.Content.ReadAsStringAsync();
            var score = BureauResponseParser.ParseScore(body);
            return new BureauReport(loanId, bureauReference, score, _clock.UtcNow);
        }
    }
}
