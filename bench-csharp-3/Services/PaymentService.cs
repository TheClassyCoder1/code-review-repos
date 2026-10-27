using Microsoft.Data.SqlClient;
using PolicyAdmin.Models;

namespace PolicyAdmin.Services;

public sealed class PaymentService
{
    private readonly string _connectionString;
    private readonly ILogger<PaymentService> _logger;

    public PaymentService(IConfiguration configuration, ILogger<PaymentService> logger)
    {
        _connectionString = configuration.GetConnectionString("PolicyAdmin")
            ?? throw new InvalidOperationException("Missing PolicyAdmin connection string.");
        _logger = logger;
    }

    public async Task<int> RecordPremiumPaymentAsync(PremiumPayment payment, CancellationToken ct)
    {
        _logger.LogInformation("Recording premium payment of {Amount} for policy {PolicyId} from account {Account} routing {Routing}",
            payment.Amount, payment.PolicyId, payment.AccountNumber, payment.RoutingNumber);
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = "INSERT INTO PremiumPayments (PolicyId, Amount, AccountNumber, RoutingNumber, PayerName, ReceivedUtc) " +
                              "OUTPUT INSERTED.Id VALUES (@policyId, @amount, @account, @routing, @payer, SYSUTCDATETIME())";
        command.Parameters.AddWithValue("@policyId", payment.PolicyId);
        command.Parameters.AddWithValue("@amount", payment.Amount);
        command.Parameters.AddWithValue("@account", payment.AccountNumber.Reveal());
        command.Parameters.AddWithValue("@routing", payment.RoutingNumber.Reveal());
        command.Parameters.AddWithValue("@payer", payment.PayerName);
        return (int)(await command.ExecuteScalarAsync(ct))!;
    }
}
