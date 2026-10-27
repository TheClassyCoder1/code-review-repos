using Dapper;
using Payroll.Models;

namespace Payroll.Data;

public sealed class TimesheetRepository
{
    private readonly DbConnectionFactory _db;

    public TimesheetRepository(DbConnectionFactory db)
    {
        _db = db;
    }

    public async Task<IReadOnlyList<TimesheetEntry>> SearchAsync(int employeeId, string? status, DateOnly from, DateOnly to)
    {
        var sql = @"SELECT id, employee_id AS EmployeeId, work_date AS WorkDate, hours,
                           project_code AS ProjectCode, status
                    FROM timesheet_entries
                    WHERE employee_id = @employeeId AND work_date BETWEEN @from AND @to";
        if (!string.IsNullOrEmpty(status))
        {
            sql += $" AND status = '{status}'";
        }
        using var connection = _db.Open();
        var rows = await connection.QueryAsync<TimesheetEntry>(sql + " ORDER BY work_date", new { employeeId, from, to });
        return rows.AsList();
    }

    public async Task<decimal> SumApprovedHoursAsync(int employeeId, DateOnly from, DateOnly to)
    {
        using var connection = _db.Open();
        return await connection.ExecuteScalarAsync<decimal>(
            @"SELECT COALESCE(SUM(hours), 0)
              FROM timesheet_entries
              WHERE employee_id = @employeeId
                AND status = @status
                AND work_date BETWEEN @from AND @to",
            new { employeeId, status = TimesheetStatus.Approved.ToString(), from, to });
    }

    public async Task<int> InsertManyAsync(IEnumerable<TimesheetEntry> entries)
    {
        using var connection = _db.Open();
        return await connection.ExecuteAsync(
            @"INSERT INTO timesheet_entries (employee_id, work_date, hours, project_code, status)
              VALUES (@EmployeeId, @WorkDate, @Hours, @ProjectCode, 'Open')",
            entries);
    }

    public async Task<int> SetStatusAsync(long entryId, TimesheetStatus status)
    {
        using var connection = _db.Open();
        return await connection.ExecuteAsync(
            "UPDATE timesheet_entries SET status = @status WHERE id = @entryId",
            new { entryId, status = status.ToString() });
    }
}
