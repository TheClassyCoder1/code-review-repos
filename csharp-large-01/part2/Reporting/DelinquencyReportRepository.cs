using System.Data;
using System.Data.Common;

namespace Lending.Part2.Reporting;

public sealed record DelinquencyRow(string LoanId, string BranchCode, int DaysPastDue, decimal OutstandingBalance, DateOnly? LastPaymentOn);

/// <summary>Read-only queries against the reporting replica.</summary>
public sealed class DelinquencyReportRepository
{
    private readonly string _providerName;
    private readonly string _connectionString;

    public DelinquencyReportRepository(IConfiguration configuration)
    {
        _providerName = configuration["Database:ProviderInvariantName"] ?? "Npgsql";
        _connectionString = configuration["Database:Reporting"] ?? throw new InvalidOperationException("Database:Reporting is not configured.");
    }

    public async Task<IReadOnlyList<DelinquencyRow>> QueryAsync(
        int minDaysPastDue, string? branchCode, string orderByColumn, string orderDirection, int limit, int offset, CancellationToken ct)
    {
        await using var connection = DbProviderFactories.GetFactory(_providerName).CreateConnection()
            ?? throw new InvalidOperationException($"Provider {_providerName} cannot create connections.");
        connection.ConnectionString = _connectionString;
        await connection.OpenAsync(ct);

        await using var command = connection.CreateCommand();
        command.CommandText =
            "SELECT l.loan_id, l.branch_code, l.days_past_due, l.outstanding_balance, p.last_payment_on " +
            "FROM loans l LEFT JOIN payment_summary p ON p.loan_id = l.loan_id " +
            "WHERE l.days_past_due >= @minDpd AND (@branch IS NULL OR l.branch_code = @branch) " +
            $"ORDER BY {orderByColumn} {orderDirection}, l.loan_id " +
            "LIMIT @limit OFFSET @offset";
        AddParameter(command, "@minDpd", DbType.Int32, minDaysPastDue);
        AddParameter(command, "@branch", DbType.String, (object?)branchCode ?? DBNull.Value);
        AddParameter(command, "@limit", DbType.Int32, limit);
        AddParameter(command, "@offset", DbType.Int32, offset);

        var rows = new List<DelinquencyRow>();
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            DateOnly? lastPayment = reader.IsDBNull(4) ? null : DateOnly.FromDateTime(reader.GetDateTime(4));
            rows.Add(new DelinquencyRow(reader.GetString(0), reader.GetString(1), reader.GetInt32(2), reader.GetDecimal(3), lastPayment));
        }
        return rows;
    }

    private static void AddParameter(DbCommand command, string name, DbType type, object value)
    {
        var parameter = command.CreateParameter();
        parameter.ParameterName = name;
        parameter.DbType = type;
        parameter.Value = value;
        command.Parameters.Add(parameter);
    }
}
