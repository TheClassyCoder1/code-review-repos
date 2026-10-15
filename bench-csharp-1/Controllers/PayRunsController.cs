using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Payroll.Data;
using Payroll.Models;
using Payroll.Security;
using Payroll.Services;

namespace Payroll.Controllers;

[ApiController]
[Authorize]
[Route("api/payruns")]
public sealed class PayRunsController : ControllerBase
{
    private static readonly IReadOnlyDictionary<string, string> SortColumns = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
    {
        ["period"] = "period_start DESC",
        ["created"] = "created_at DESC",
        ["gross"] = "total_gross DESC",
        ["status"] = "status, period_start DESC"
    };

    private readonly PayRunRepository _payRuns;
    private readonly WebhookNotifier _notifier;
    private readonly BankDirectory _bankDirectory;
    private readonly BankHolidayClient _holidays;

    public PayRunsController(PayRunRepository payRuns, WebhookNotifier notifier, BankDirectory bankDirectory, BankHolidayClient holidays)
    {
        _payRuns = payRuns;
        _notifier = notifier;
        _bankDirectory = bankDirectory;
        _holidays = holidays;
    }

    [HttpGet]
    public async Task<IActionResult> List([FromQuery] string? sort, [FromQuery] int page = 1)
    {
        if (!SortColumns.TryGetValue(sort ?? "period", out var orderBy))
        {
            return BadRequest(new { error = $"sort must be one of {string.Join(", ", SortColumns.Keys)}" });
        }
        var runs = await _payRuns.ListAsync(User.GetCompanyId(), orderBy, page, 50);
        return Ok(runs);
    }

    [HttpPost]
    [Authorize(Roles = "PayrollAdmin")]
    public async Task<IActionResult> Create([FromBody] CreatePayRunRequest request, CancellationToken ct)
    {
        if (request.PeriodEnd < request.PeriodStart)
        {
            return BadRequest(new { error = "period_end must not be before period_start" });
        }
        var id = await _payRuns.InsertAsync(User.GetCompanyId(), request.PeriodStart, request.PeriodEnd);
        var run = await _payRuns.GetAsync(id);
        if (run is not null && !string.IsNullOrWhiteSpace(request.CallbackUrl))
        {
            await _notifier.NotifyPayRunCreatedAsync(request.CallbackUrl, run, ct);
        }
        return CreatedAtAction(nameof(GetMyLines), new { payRunId = id }, run);
    }

    [HttpGet("{payRunId:guid}/my-lines")]
    public async Task<IActionResult> GetMyLines(Guid payRunId)
    {
        var run = await _payRuns.GetAsync(payRunId);
        if (run is null || run.CompanyId != User.GetCompanyId())
        {
            return NotFound();
        }
        var lines = await _payRuns.GetLinesAsync(payRunId, User.GetEmployeeId());
        return Ok(lines);
    }

    [HttpGet("{payRunId:guid}/badge")]
    public async Task<IActionResult> GetStatusBadge(Guid payRunId)
    {
        var run = await _payRuns.GetAsync(payRunId);
        if (run is null || run.CompanyId != User.GetCompanyId())
        {
            return NotFound();
        }
        return Content(PayslipRenderer.RenderStatusBadge(run.Status.ToString()), "text/html");
    }

    [HttpGet("{payRunId:guid}/employees/{employeeId:int}/lines")]
    public async Task<IActionResult> GetEmployeeLines(Guid payRunId, int employeeId)
    {
        var run = await _payRuns.GetAsync(payRunId);
        if (run is null)
        {
            return NotFound();
        }
        var lines = await _payRuns.GetLinesAsync(payRunId, employeeId);
        return Ok(lines);
    }

    [HttpGet("holidays/{bankCode}")]
    public async Task<IActionResult> GetBankHolidays(string bankCode, [FromQuery] int? year, CancellationToken ct)
    {
        if (!_bankDirectory.TryResolveHost(bankCode, out var host))
        {
            return BadRequest(new { error = $"unknown bank code, expected one of {string.Join(", ", _bankDirectory.KnownBankCodes)}" });
        }
        var holidays = await _holidays.FetchHolidaysAsync(host, year ?? DateTime.UtcNow.Year, ct);
        return Ok(holidays);
    }
}
