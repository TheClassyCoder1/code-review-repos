using Dapper;

namespace Payroll.Data;

public sealed class ApiKeyRepository
{
    private readonly DbConnectionFactory _db;

    public ApiKeyRepository(DbConnectionFactory db)
    {
        _db = db;
    }

    public async Task<Guid> InsertAsync(int companyId, string name, string keyHash)
    {
        var id = Guid.NewGuid();
        using var connection = _db.Open();
        await connection.ExecuteAsync(
            @"INSERT INTO api_keys (id, company_id, name, key_hash, created_at)
              VALUES (@id, @companyId, @name, @keyHash, now())",
            new { id, companyId, name, keyHash });
        return id;
    }
}
