using Dapper;
using Payroll.Models;

namespace Payroll.Data;

public sealed class PayRunRepository
{
    private readonly DbConnectionFactory _db;

    public PayRunRepository(DbConnectionFactory db)
    {
        _db = db;
    }

    public async Task<IReadOnlyList<PayRun>> ListAsync(int companyId, string orderBy, int page, int pageSize)
    {
        var offset = (Math.Max(page, 1) - 1) * pageSize;
        var sql = $@"SELECT id, company_id AS CompanyId, period_start AS PeriodStart, period_end AS PeriodEnd,
                            status, total_gross AS TotalGross, created_at AS CreatedAt
                     FROM pay_runs
                     WHERE company_id = @companyId
                     ORDER BY {orderBy}
                     LIMIT @pageSize OFFSET @offset";
        using var connection = _db.Open();
        var rows = await connection.QueryAsync<PayRun>(sql, new { companyId, pageSize, offset });
        return rows.AsList();
    }

    public async Task<Guid> InsertAsync(int companyId, DateOnly periodStart, DateOnly periodEnd)
    {
        var id = Guid.NewGuid();
        using var connection = _db.Open();
        await connection.ExecuteAsync(
            @"INSERT INTO pay_runs (id, company_id, period_start, period_end, status, total_gross, created_at)
              VALUES (@id, @companyId, @periodStart, @periodEnd, @status, 0, now())",
            new { id, companyId, periodStart, periodEnd, status = PayRunStatus.Draft.ToString() });
        return id;
    }

    public async Task<PayRun?> GetAsync(Guid payRunId)
    {
        using var connection = _db.Open();
        return await connection.QuerySingleOrDefaultAsync<PayRun>(
            @"SELECT id, company_id AS CompanyId, period_start AS PeriodStart, period_end AS PeriodEnd,
                     status, total_gross AS TotalGross, created_at AS CreatedAt
              FROM pay_runs WHERE id = @payRunId",
            new { payRunId });
    }

    public async Task<IReadOnlyList<PayslipLine>> GetLinesAsync(Guid payRunId, int employeeId)
    {
        using var connection = _db.Open();
        var rows = await connection.QueryAsync<PayslipLine>(
            @"SELECT pay_run_id AS PayRunId, employee_id AS EmployeeId, description, amount
              FROM payslip_lines
              WHERE pay_run_id = @payRunId AND employee_id = @employeeId
              ORDER BY line_no",
            new { payRunId, employeeId });
        return rows.AsList();
    }

    public async Task InsertShareAsync(string code, Guid payRunId, int employeeId)
    {
        using var connection = _db.Open();
        await connection.ExecuteAsync(
            "INSERT INTO payslip_shares (code, pay_run_id, employee_id, created_at) VALUES (@code, @payRunId, @employeeId, now())",
            new { code, payRunId, employeeId });
    }

    public async Task<PayslipShare?> FindShareAsync(string code)
    {
        using var connection = _db.Open();
        return await connection.QuerySingleOrDefaultAsync<PayslipShare>(
            "SELECT pay_run_id AS PayRunId, employee_id AS EmployeeId FROM payslip_shares WHERE code = @code",
            new { code });
    }
}
