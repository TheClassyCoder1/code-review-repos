using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part2.Exports;

public sealed record ExportRequest(string PartnerCode, List<ExportRow> Rows);

[ApiController]
[Authorize(Roles = "servicing-ops")]
[Route("servicing/exports")]
public sealed class ExportsController : ControllerBase
{
    private readonly PartnerExportService _exports;

    public ExportsController(PartnerExportService exports)
    {
        _exports = exports;
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] ExportRequest request, CancellationToken ct)
    {
        try
        {
            var path = await _exports.ExportAsync(request.PartnerCode, request.Rows, ct);
            return Ok(new { file = Path.GetFileName(path) });
        }
        catch (UnknownExportPartnerException)
        {
            return UnprocessableEntity(new { error = "unknown partner" });
        }
    }
}
