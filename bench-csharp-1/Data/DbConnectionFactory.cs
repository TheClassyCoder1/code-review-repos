using System.Data;
using Npgsql;

namespace Payroll.Data;

public sealed class DbConnectionFactory
{
    private readonly string _connectionString;

    public DbConnectionFactory(IConfiguration configuration)
    {
        _connectionString = configuration.GetConnectionString("Payroll")
            ?? throw new InvalidOperationException("ConnectionStrings:Payroll is not set");
    }

    public IDbConnection Open()
    {
        var connection = new NpgsqlConnection(_connectionString);
        connection.Open();
        return connection;
    }
}
