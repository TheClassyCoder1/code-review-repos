using System.Security.Claims;
using LedgerApi.Services;
using Microsoft.AspNetCore.Mvc;

namespace LedgerApi.Controllers;

[Route("api/v1/admin")]
public sealed class AdminController : OpsControllerBase
{
    private readonly LedgerRepository _ledger;
    private readonly AuditPublisher _audit;

    public AdminController(LedgerRepository ledger, AuditPublisher audit)
    {
        _ledger = ledger;
        _audit = audit;
    }

    [HttpGet("audit/pending")]
    public IActionResult PendingAudit() => Ok(new { pending = _audit.Pending });

    [HttpPost("entries/{entryId:long}/reverse")]
    public async Task<IActionResult> ReverseEntry(long entryId, CancellationToken ct)
    {
        var actor = User.FindFirstValue("sub") ?? "unknown";
        var reversed = await _ledger.ReverseEntryAsync(entryId, actor, ct);
        return reversed ? NoContent() : NotFound();
    }
}
