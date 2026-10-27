using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Payroll.Data;
using Payroll.Security;
using Payroll.Services;

namespace Payroll.Controllers;

[ApiController]
[Authorize]
[Route("api/timesheets")]
public sealed class TimesheetsController : ControllerBase
{
    private readonly TimesheetRepository _timesheets;
    private readonly TimesheetImportService _imports;

    public TimesheetsController(TimesheetRepository timesheets, TimesheetImportService imports)
    {
        _timesheets = timesheets;
        _imports = imports;
    }

    [HttpGet]
    public async Task<IActionResult> Search([FromQuery] string? status, [FromQuery] DateOnly from, [FromQuery] DateOnly to)
    {
        var entries = await _timesheets.SearchAsync(User.GetEmployeeId(), status, from, to);
        return Ok(entries);
    }

    [HttpPost("import/legacy")]
    public async Task<IActionResult> ImportLegacy()
    {
        using var reader = new StreamReader(Request.Body);
        var json = await reader.ReadToEndAsync();
        var count = await _imports.ImportLegacyExportAsync(User.GetEmployeeId(), json);
        return Ok(new { imported = count });
    }

    [HttpPost("import/template")]
    public async Task<IActionResult> ImportTemplate()
    {
        using var reader = new StreamReader(Request.Body);
        var json = await reader.ReadToEndAsync();
        var count = await _imports.ImportTemplateAsync(User.GetEmployeeId(), json);
        return Ok(new { imported = count });
    }
}
