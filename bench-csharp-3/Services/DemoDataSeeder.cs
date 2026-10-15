using Microsoft.Data.SqlClient;
using PolicyAdmin.Security;

namespace PolicyAdmin.Services;

public sealed class DemoDataSeeder
{
    private readonly string _connectionString;
    private readonly PasswordService _passwords;

    public DemoDataSeeder(IConfiguration configuration, PasswordService passwords)
    {
        _connectionString = configuration.GetConnectionString("PolicyAdmin")
            ?? throw new InvalidOperationException("Missing PolicyAdmin connection string.");
        _passwords = passwords;
    }

    public async Task SeedDemoAgentAsync(CancellationToken ct)
    {
        const string demoEmail = "demo.agent@policyadmin.local";
        const string demoPassword = "Harbor-Lantern-2026!";
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = "IF NOT EXISTS (SELECT 1 FROM Agents WHERE Email = @email) " +
                              "INSERT INTO Agents (Email, DisplayName, PasswordHash, LicenseNumber, LicenseExpires, CommissionRate) " +
                              "VALUES (@email, 'Demo Agent', @hash, 'DEMO-0001', '2030-12-31', 0.10)";
        command.Parameters.AddWithValue("@email", demoEmail);
        command.Parameters.AddWithValue("@hash", _passwords.HashPassword(demoPassword));
        await command.ExecuteNonQueryAsync(ct);
    }
}
