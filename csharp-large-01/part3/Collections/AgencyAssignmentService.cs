using System.Collections.Concurrent;
using Lending.Servicing.Part3.Domain;
using Lending.Servicing.Part3.Infrastructure;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Collections;

/// <summary>
/// Places charge-off cases with a recovery agency. A loan may be with at most one agency
/// at a time; double placement means two agencies contacting the same borrower.
/// </summary>
public sealed class AgencyAssignmentService
{
    private static readonly string[] AgencyRotation = { "AGY-NORTH", "AGY-CENTRAL", "AGY-SOUTH" };

    private readonly ConcurrentDictionary<string, AgencyAssignment> _assignments = new(StringComparer.Ordinal);
    private readonly AgencyPlacementClient _client;
    private readonly IAccountStore _accounts;
    private readonly ILogger<AgencyAssignmentService> _logger;
    private int _rotation;

    public AgencyAssignmentService(
        AgencyPlacementClient client,
        IAccountStore accounts,
        ILogger<AgencyAssignmentService> logger)
    {
        _client = client;
        _accounts = accounts;
        _logger = logger;
    }

    /// <summary>
    /// Places the case with the next agency in rotation unless the loan is already placed.
    /// Called by the charge-off sweep and by the manual "send to agency" action.
    /// </summary>
    public async Task<AgencyAssignment?> PlaceAsync(DelinquencyCase delinquencyCase, CancellationToken cancellationToken)
    {
        if (delinquencyCase.Stage != DelinquencyStage.ChargeOff)
        {
            return null;
        }

        if (_assignments.TryGetValue(delinquencyCase.LoanId, out var existing))
        {
            return existing;
        }

        var account = await _accounts.GetAsync(delinquencyCase.LoanId, cancellationToken);
        if (account is null)
        {
            return null;
        }

        var agency = AgencyRotation[(Interlocked.Increment(ref _rotation) & int.MaxValue) % AgencyRotation.Length];
        var assignment = await _client.PlaceAsync(delinquencyCase, account.TotalOutstanding, agency);
        _assignments[delinquencyCase.LoanId] = assignment;

        _logger.LogInformation("Placed case {CaseId} for loan {LoanId} with {Agency} as {AssignmentId}",
            delinquencyCase.CaseId, delinquencyCase.LoanId, agency, assignment.AssignmentId);
        return assignment;
    }
}
