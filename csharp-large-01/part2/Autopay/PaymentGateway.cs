namespace Lending.Part2.Autopay;

public sealed record DebitResult(string TransactionId, bool Approved, string? DeclineCode);

public interface IPaymentGateway
{
    Task<DebitResult> DebitAsync(string fundingAccountToken, decimal amount, string idempotencyKey, CancellationToken ct);
}

/// <summary>Stand-in gateway used outside production; approves every debit.</summary>
public sealed class LoggingPaymentGateway : IPaymentGateway
{
    private readonly ILogger<LoggingPaymentGateway> _logger;

    public LoggingPaymentGateway(ILogger<LoggingPaymentGateway> logger) => _logger = logger;

    public Task<DebitResult> DebitAsync(string fundingAccountToken, decimal amount, string idempotencyKey, CancellationToken ct)
    {
        _logger.LogInformation("Simulated debit {IdempotencyKey} for {Amount}", idempotencyKey, amount);
        return Task.FromResult(new DebitResult($"sim-{Guid.NewGuid():N}", true, null));
    }
}
