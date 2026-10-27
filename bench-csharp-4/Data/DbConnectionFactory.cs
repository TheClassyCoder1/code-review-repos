using System.Data.Common;
using Npgsql;

namespace GiftCards.Data;

public interface IDbConnectionFactory
{
    DbConnection Create();
}

public sealed class NpgsqlConnectionFactory : IDbConnectionFactory
{
    private readonly string _connectionString;

    public NpgsqlConnectionFactory(string connectionString) => _connectionString = connectionString;

    public DbConnection Create() => new NpgsqlConnection(_connectionString);
}
