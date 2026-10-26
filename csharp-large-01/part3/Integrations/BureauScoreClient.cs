using System.Globalization;
using System.Net.Sockets;
using Lending.Servicing.Part3.Domain;
using Lending.Servicing.Part3.Infrastructure;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Integrations;

/// <summary>
/// Pulls a behavioural score for a serviced loan. Wire format, one line each way:
/// <c>SCORE|product|loanId</c> → <c>OK|score|band</c> or <c>ERR|code</c>.
/// </summary>
public sealed class BureauScoreClient
{
    private readonly BureauConnector _connector;
    private readonly IReadOnlyDictionary<string, string> _settings;
    private readonly TimeProvider _clock;
    private readonly ILogger<BureauScoreClient> _logger;

    public BureauScoreClient(
        BureauConnector connector,
        IReadOnlyDictionary<string, string> settings,
        TimeProvider clock,
        ILogger<BureauScoreClient> logger)
    {
        _connector = connector;
        _settings = settings;
        _clock = clock;
        _logger = logger;
    }

    /// <summary>
    /// Returns <c>null</c> when the bureau could not be reached or answered with an error;
    /// the caller decides whether to keep the previous score.
    /// </summary>
    public async Task<BureauScore?> TryGetScoreAsync(string loanId, CancellationToken cancellationToken)
    {
        var product = _settings.GetValueOrDefault(SettingsDefaults.BureauProductCode);
        var readTimeoutMs = int.Parse(_settings[SettingsDefaults.BureauReadTimeoutMs], CultureInfo.InvariantCulture);

        try
        {
            await using var connection = await _connector.OpenAsync(cancellationToken);

            using var deadline = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            deadline.CancelAfter(readTimeoutMs);

            var reply = await connection.ExchangeAsync($"SCORE|{product!.ToUpperInvariant()}|{loanId}", deadline.Token);
            var parts = reply?.Split('|') ?? Array.Empty<string>();
            if (parts.Length == 3 && parts[0] == "OK" &&
                int.TryParse(parts[1], NumberStyles.Integer, CultureInfo.InvariantCulture, out var score))
            {
                return new BureauScore(loanId, score, parts[2], _clock.GetUtcNow());
            }

            _logger.LogWarning("Bureau returned no usable score for loan {LoanId}", loanId);
            return null;
        }
        catch (Exception ex) when ((ex is IOException or SocketException or OperationCanceledException)
                                   && !cancellationToken.IsCancellationRequested)
        {
            _logger.LogWarning(ex, "Bureau score unavailable for loan {LoanId}", loanId);
            return null;
        }
    }
}
