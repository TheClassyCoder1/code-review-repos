using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Payroll.Data;
using Payroll.Security;
using Payroll.Services;

namespace Payroll.Controllers;

[ApiController]
[Authorize]
[Route("api/payslips")]
public sealed class PayslipsController : ControllerBase
{
    private readonly PayslipStore _store;
    private readonly PayslipLinkService _links;
    private readonly PayRunRepository _payRuns;
    private readonly EmployeeRepository _employees;
    private readonly PayslipRenderer _renderer;

    public PayslipsController(PayslipStore store, PayslipLinkService links, PayRunRepository payRuns,
        EmployeeRepository employees, PayslipRenderer renderer)
    {
        _store = store;
        _links = links;
        _payRuns = payRuns;
        _employees = employees;
        _renderer = renderer;
    }

    [HttpGet("statements")]
    public IActionResult DownloadStatement([FromQuery] string fileName)
    {
        var statementsDir = Path.Combine(_store.RootDirectory, "statements", User.GetEmployeeId().ToString());
        var fullPath = Path.Combine(statementsDir, fileName);
        if (!System.IO.File.Exists(fullPath))
        {
            return NotFound();
        }
        return PhysicalFile(fullPath, "application/pdf", fileName);
    }

    [HttpGet("{payRunId:guid}.pdf")]
    public async Task<IActionResult> GetPdf(Guid payRunId, CancellationToken ct)
    {
        var bytes = await _store.ReadAsync($"{User.GetEmployeeId()}/{payRunId:N}.pdf", ct);
        return bytes is null ? NotFound() : File(bytes, "application/pdf");
    }

    [HttpGet("{payRunId:guid}/view")]
    public async Task<IActionResult> ViewHtml(Guid payRunId)
    {
        var run = await _payRuns.GetAsync(payRunId);
        var employee = await _employees.GetByIdAsync(User.GetCompanyId(), User.GetEmployeeId());
        if (run is null || employee is null || run.CompanyId != employee.CompanyId)
        {
            return NotFound();
        }
        var lines = await _payRuns.GetLinesAsync(payRunId, employee.Id);
        return Content(_renderer.RenderPage(employee, run, lines), "text/html");
    }

    [HttpPost("{payRunId:guid}/share")]
    public async Task<IActionResult> Share(Guid payRunId)
    {
        var code = _links.CreateShareCode();
        await _payRuns.InsertShareAsync(code, payRunId, User.GetEmployeeId());
        return Ok(new { url = _links.BuildShareUrl(code, TimeSpan.FromDays(7)) });
    }

    [AllowAnonymous]
    [HttpGet("shared/{code}")]
    public async Task<IActionResult> OpenShared(string code, [FromQuery] long exp, [FromQuery] string sig, CancellationToken ct)
    {
        if (!_links.IsValidSignature(code, exp, sig))
        {
            return NotFound();
        }
        var share = await _payRuns.FindShareAsync(code);
        if (share is null)
        {
            return NotFound();
        }
        var bytes = await _store.ReadAsync($"{share.EmployeeId}/{share.PayRunId:N}.pdf", ct);
        return bytes is null ? NotFound() : File(bytes, "application/pdf");
    }
}
