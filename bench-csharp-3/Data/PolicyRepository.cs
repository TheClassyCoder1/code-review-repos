using Microsoft.Data.SqlClient;
using PolicyAdmin.Models;

namespace PolicyAdmin.Data;

public sealed class PolicyRepository
{
    private const string PolicyColumns =
        "Id, PolicyNumber, AgentId, InsuredName, LineOfBusiness, Status, EffectiveDate, ExpirationDate, AnnualPremium, UnderwritingNotes";

    private readonly string _connectionString;

    public PolicyRepository(IConfiguration configuration)
    {
        _connectionString = configuration.GetConnectionString("PolicyAdmin")
            ?? throw new InvalidOperationException("Missing PolicyAdmin connection string.");
    }

    public async Task<Policy?> GetByIdForAgentAsync(int policyId, int agentId, CancellationToken ct)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = $"SELECT {PolicyColumns} FROM Policies WHERE Id = @id AND AgentId = @agentId";
        command.Parameters.AddWithValue("@id", policyId);
        command.Parameters.AddWithValue("@agentId", agentId);
        await using var reader = await command.ExecuteReaderAsync(ct);
        return await reader.ReadAsync(ct) ? MapPolicy(reader) : null;
    }

    public async Task<Policy?> GetByIdAsync(int policyId, CancellationToken ct)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = $"SELECT {PolicyColumns} FROM Policies WHERE Id = @id";
        command.Parameters.AddWithValue("@id", policyId);
        await using var reader = await command.ExecuteReaderAsync(ct);
        return await reader.ReadAsync(ct) ? MapPolicy(reader) : null;
    }

    public async Task<IReadOnlyList<Policy>> SearchByInsuredAsync(int agentId, string insuredPrefix, CancellationToken ct)
    {
        var policies = new List<Policy>();
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = $"SELECT {PolicyColumns} FROM Policies WHERE AgentId = @agentId " +
                              "AND InsuredName LIKE '" + insuredPrefix + "%' ORDER BY InsuredName";
        command.Parameters.AddWithValue("@agentId", agentId);
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            policies.Add(MapPolicy(reader));
        }
        return policies;
    }

    public async Task UpdateStatusAsync(int policyId, PolicyStatus status, CancellationToken ct)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = "UPDATE Policies SET Status = @status WHERE Id = @id";
        command.Parameters.AddWithValue("@status", (int)status);
        command.Parameters.AddWithValue("@id", policyId);
        await command.ExecuteNonQueryAsync(ct);
    }

    public async Task<IReadOnlyDictionary<string, decimal>> GetBaseRatesAsync(string lineName, string stateCode, CancellationToken ct)
    {
        var rates = new Dictionary<string, decimal>(StringComparer.OrdinalIgnoreCase);
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = $"SELECT CoverageCode, BaseRate FROM Rates_{lineName} WHERE StateCode = @state";
        command.Parameters.AddWithValue("@state", stateCode);
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            rates[reader.GetString(0)] = reader.GetDecimal(1);
        }
        return rates;
    }

    private static Policy MapPolicy(SqlDataReader reader) => new()
    {
        Id = reader.GetInt32(0),
        PolicyNumber = reader.GetString(1),
        AgentId = reader.GetInt32(2),
        InsuredName = reader.GetString(3),
        Line = (LineOfBusiness)reader.GetInt32(4),
        Status = (PolicyStatus)reader.GetInt32(5),
        EffectiveDate = DateOnly.FromDateTime(reader.GetDateTime(6)),
        ExpirationDate = DateOnly.FromDateTime(reader.GetDateTime(7)),
        AnnualPremium = reader.GetDecimal(8),
        UnderwritingNotes = reader.IsDBNull(9) ? null : reader.GetString(9)
    };
}
