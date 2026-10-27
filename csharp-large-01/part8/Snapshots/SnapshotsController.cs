using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part8.Snapshots;

[ApiController]
[Authorize(Roles = "servicing-ops")]
[Route("ops/snapshots")]
public sealed class SnapshotsController(SnapshotService snapshots) : ControllerBase
{
    [HttpPost("{table}")]
    public async Task<IActionResult> Create(string table, CancellationToken ct)
    {
        var file = await snapshots.SnapshotAsync(table, ct);
        return file is null ? NotFound() : Accepted(new { file = Path.GetFileName(file) });
    }
}
