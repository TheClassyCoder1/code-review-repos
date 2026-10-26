using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part2.Reporting;

[ApiController]
[Authorize(Roles = "servicing-ops,collections")]
[Route("servicing/reports")]
public sealed class ReportsController(DelinquencyReportService reports) : ControllerBase
{
    [HttpGet("delinquency")]
    public async Task<IActionResult> Delinquency(
        [FromQuery] int minDpd = 30, [FromQuery] string? branch = null, [FromQuery] string? sort = null,
        [FromQuery] string? dir = null, [FromQuery] int page = 1, [FromQuery] int pageSize = 100, CancellationToken ct = default) =>
        Ok(await reports.GetPageAsync(new DelinquencyReportQuery(minDpd, branch, sort, dir, page, pageSize), ct));
}
