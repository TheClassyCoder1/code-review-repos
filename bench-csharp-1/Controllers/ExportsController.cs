using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Payroll.Services;

namespace Payroll.Controllers;

[ApiController]
[Authorize(Roles = "PayrollAdmin")]
[Route("api/exports")]
public sealed class ExportsController : ControllerBase
{
    private readonly BankFileService _bankFiles;

    public ExportsController(BankFileService bankFiles)
    {
        _bankFiles = bankFiles;
    }

    [HttpPost("{payRunId:guid}/sign")]
    public async Task<IActionResult> Sign(Guid payRunId, [FromQuery] string? label)
    {
        var batchLabel = string.IsNullOrWhiteSpace(label) ? payRunId.ToString() : label;
        var exitCode = await _bankFiles.SignBatchAsync(batchLabel);
        return exitCode == 0 ? Ok(new { signed = batchLabel }) : StatusCode(500, new { error = "signing failed" });
    }

    [HttpPost("{batchId:guid}/archive")]
    public async Task<IActionResult> Archive(Guid batchId)
    {
        var archive = await _bankFiles.CompressBatchAsync(batchId.ToString());
        return Ok(new { archive = Path.GetFileName(archive) });
    }
}
