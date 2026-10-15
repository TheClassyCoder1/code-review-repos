using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using Payroll.Data;
using Payroll.Models;

namespace Payroll.Services;

public abstract class TemplateRow
{
    public DateOnly WorkDate { get; set; }
}

public sealed class HoursTemplateRow : TemplateRow
{
    public decimal Hours { get; set; }
    public string ProjectCode { get; set; } = "";
}

public sealed class AllowanceTemplateRow : TemplateRow
{
    public string AllowanceCode { get; set; } = "";
}

public sealed class LeaveTemplateRow : TemplateRow
{
    public string LeaveType { get; set; } = "";
    public decimal Hours { get; set; }
}

public sealed class TimesheetImportService
{
    private readonly TimesheetRepository _timesheets;
    private readonly ISerializationBinder _templateBinder;

    public TimesheetImportService(TimesheetRepository timesheets, ISerializationBinder templateBinder)
    {
        _timesheets = timesheets;
        _templateBinder = templateBinder;
    }

    public async Task<int> ImportLegacyExportAsync(int employeeId, string json)
    {
        var settings = new JsonSerializerSettings { TypeNameHandling = TypeNameHandling.All };
        var entries = JsonConvert.DeserializeObject<List<TimesheetEntry>>(json, settings) ?? new List<TimesheetEntry>();
        foreach (var entry in entries)
        {
            entry.EmployeeId = employeeId;
        }
        ValidateEntries(entries);
        return await _timesheets.InsertManyAsync(entries);
    }

    private static TimesheetEntry ToEntry(int employeeId, HoursTemplateRow row) => new()
    {
        EmployeeId = employeeId,
        WorkDate = row.WorkDate,
        Hours = row.Hours,
        ProjectCode = row.ProjectCode
    };

    public async Task<int> ImportTemplateAsync(int employeeId, string json)
    {
        var settings = new JsonSerializerSettings
        {
            TypeNameHandling = TypeNameHandling.Auto,
            SerializationBinder = _templateBinder
        };
        var rows = JsonConvert.DeserializeObject<List<TemplateRow>>(json, settings) ?? new List<TemplateRow>();
        var entries = rows.OfType<HoursTemplateRow>().Select(r => ToEntry(employeeId, r)).ToList();
        ValidateEntries(entries);
        return await _timesheets.InsertManyAsync(entries);
    }

    public static decimal TotalHours(IEnumerable<TimesheetEntry> entries)
    {
        return entries.Sum(e => e.Hours);
    }

    public static void ValidateEntries(IReadOnlyCollection<TimesheetEntry> entries)
    {
        foreach (var entry in entries)
        {
            if (entry.Hours <= 0 || entry.Hours > 24)
            {
                throw new ArgumentException($"Hours for {entry.WorkDate:yyyy-MM-dd} must be between 0 and 24");
            }
            if (string.IsNullOrWhiteSpace(entry.ProjectCode))
            {
                throw new ArgumentException($"Project code is required for {entry.WorkDate:yyyy-MM-dd}");
            }
        }
    }
}
