using GiftCards.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GiftCards.Controllers;

[ApiController]
[Authorize(Roles = "merchant")]
[Route("api/reports")]
public sealed class ReportsController : ControllerBase
{
    private readonly ReportService _reports;

    public ReportsController(ReportService reports) => _reports = reports;

    [HttpGet("redemptions.csv")]
    public async Task<IActionResult> RedemptionsCsv(CancellationToken ct)
    {
        var merchantId = User.GetMerchantId();
        if (merchantId is null)
        {
            return Forbid();
        }
        var csv = await _reports.ExportMerchantRedemptionsCsvAsync(merchantId, ct);
        return Content(csv, "text/csv");
    }

    [HttpGet("settlements/{businessDate}")]
    [Authorize(Roles = "finance")]
    public IActionResult Settlement(DateOnly businessDate)
    {
        if (businessDate > DateOnly.FromDateTime(DateTime.UtcNow))
        {
            return BadRequest();
        }
        var stream = _reports.OpenSettlementFile(businessDate);
        return File(stream, "text/csv", ReportService.SettlementFileName(businessDate));
    }
}
