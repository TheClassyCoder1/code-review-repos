using Microsoft.Data.SqlClient;
using PolicyAdmin.Models;

namespace PolicyAdmin.Data;

public sealed class EndorsementRepository
{
    private readonly string _connectionString;

    public EndorsementRepository(IConfiguration configuration)
    {
        _connectionString = configuration.GetConnectionString("PolicyAdmin")
            ?? throw new InvalidOperationException("Missing PolicyAdmin connection string.");
    }

    public async Task<IReadOnlyList<Endorsement>> ListForPolicyAsync(int policyId, CancellationToken ct)
    {
        var endorsements = new List<Endorsement>();
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = "SELECT Id, PolicyId, Code, Description, EffectiveDate, PremiumDelta, CreatedByAgentId " +
                              "FROM Endorsements WHERE PolicyId = @policyId ORDER BY EffectiveDate, Id";
        command.Parameters.AddWithValue("@policyId", policyId);
        await using var reader = await command.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            endorsements.Add(MapEndorsement(reader));
        }
        return endorsements;
    }

    public async Task<int> AddWithPremiumAdjustmentAsync(Endorsement endorsement, CancellationToken ct)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var transaction = (SqlTransaction)await connection.BeginTransactionAsync(ct);

        await using var insert = connection.CreateCommand();
        insert.Transaction = transaction;
        insert.CommandText = "INSERT INTO Endorsements (PolicyId, Code, Description, EffectiveDate, PremiumDelta, CreatedByAgentId) " +
                             "OUTPUT INSERTED.Id VALUES (@policyId, @code, @description, @effective, @delta, @agentId)";
        insert.Parameters.AddWithValue("@policyId", endorsement.PolicyId);
        insert.Parameters.AddWithValue("@code", endorsement.Code);
        insert.Parameters.AddWithValue("@description", endorsement.Description);
        insert.Parameters.AddWithValue("@effective", endorsement.EffectiveDate.ToDateTime(TimeOnly.MinValue));
        insert.Parameters.AddWithValue("@delta", endorsement.PremiumDelta);
        insert.Parameters.AddWithValue("@agentId", endorsement.CreatedByAgentId);
        var id = (int)(await insert.ExecuteScalarAsync(ct))!;

        await using var update = connection.CreateCommand();
        update.Transaction = transaction;
        update.CommandText = "UPDATE Policies SET AnnualPremium = AnnualPremium + @delta WHERE Id = @policyId";
        update.Parameters.AddWithValue("@delta", endorsement.PremiumDelta);
        update.Parameters.AddWithValue("@policyId", endorsement.PolicyId);
        await update.ExecuteNonQueryAsync(ct);

        await transaction.CommitAsync(ct);
        return id;
    }

    public async Task<Endorsement?> GetByIdAsync(int endorsementId, CancellationToken ct)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = "SELECT Id, PolicyId, Code, Description, EffectiveDate, PremiumDelta, CreatedByAgentId " +
                              "FROM Endorsements WHERE Id = @id";
        command.Parameters.AddWithValue("@id", endorsementId);
        await using var reader = await command.ExecuteReaderAsync(ct);
        if (!await reader.ReadAsync(ct))
        {
            return null;
        }
        return MapEndorsement(reader);
    }

    private static Endorsement MapEndorsement(SqlDataReader reader) => new()
    {
        Id = reader.GetInt32(0),
        PolicyId = reader.GetInt32(1),
        Code = reader.GetString(2),
        Description = reader.GetString(3),
        EffectiveDate = DateOnly.FromDateTime(reader.GetDateTime(4)),
        PremiumDelta = reader.GetDecimal(5),
        CreatedByAgentId = reader.GetInt32(6)
    };
}
