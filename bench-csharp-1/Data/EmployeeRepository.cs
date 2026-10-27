using Dapper;
using Payroll.Models;

namespace Payroll.Data;

public sealed class EmployeeRepository
{
    private readonly DbConnectionFactory _db;

    public EmployeeRepository(DbConnectionFactory db)
    {
        _db = db;
    }

    public async Task<Employee?> GetByIdAsync(int companyId, int employeeId)
    {
        using var connection = _db.Open();
        return await connection.QuerySingleOrDefaultAsync<Employee>(
            @"SELECT id, company_id AS CompanyId, full_name AS FullName, email, department,
                     annual_salary AS AnnualSalary, routing_number AS RoutingNumber,
                     account_number AS AccountNumber
              FROM employees
              WHERE company_id = @companyId AND id = @employeeId",
            new { companyId, employeeId });
    }

    public async Task<IReadOnlyList<Employee>> ListByDepartmentAsync(int companyId, string department)
    {
        using var connection = _db.Open();
        var rows = await connection.QueryAsync<Employee>(
            @"SELECT id, company_id AS CompanyId, full_name AS FullName, email, department,
                     annual_salary AS AnnualSalary
              FROM employees
              WHERE company_id = @companyId AND department = @department
              ORDER BY full_name",
            new { companyId, department });
        return rows.AsList();
    }

    public async Task<int?> FindIdByEmailAsync(string email)
    {
        using var connection = _db.Open();
        return await connection.QuerySingleOrDefaultAsync<int?>(
            "SELECT id FROM employees WHERE lower(email) = lower(@email)",
            new { email });
    }

    public async Task<EmployeeCredentials?> GetCredentialsAsync(string email)
    {
        using var connection = _db.Open();
        return await connection.QuerySingleOrDefaultAsync<EmployeeCredentials>(
            "SELECT id AS EmployeeId, company_id AS CompanyId, password_hash AS PasswordHash FROM employees WHERE lower(email) = lower(@email)",
            new { email });
    }

    public async Task SaveResetTokenAsync(int employeeId, string tokenHash, DateTime expiresAtUtc)
    {
        using var connection = _db.Open();
        await connection.ExecuteAsync(
            @"INSERT INTO password_resets (employee_id, token_hash, expires_at)
              VALUES (@employeeId, @tokenHash, @expiresAtUtc)",
            new { employeeId, tokenHash, expiresAtUtc });
    }
}
