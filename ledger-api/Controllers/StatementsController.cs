using LedgerApi.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace LedgerApi.Controllers;

[ApiController]
[Authorize(Policy = "AccountOwner")]
[Route("api/v1/accounts/{accountId}/statements")]
public sealed class StatementsController : ControllerBase
{
    private readonly ImportService _imports;
    private readonly LedgerRepository _ledger;

    public StatementsController(ImportService imports, LedgerRepository ledger)
    {
        _imports = imports;
        _ledger = ledger;
    }

    [HttpPost("import")]
    [Consumes("application/json")]
    public async Task<IActionResult> Import(string accountId, CancellationToken ct)
    {
        using var reader = new StreamReader(Request.Body);
        var json = await reader.ReadToEndAsync(ct);
        var batch = _imports.ReadBatch(json);
        _imports.ValidateBatch(batch, accountId);
        var inserted = await _ledger.InsertEntriesAsync(accountId, batch.Entries, ct);
        return Ok(new { batch_id = batch.BatchId, inserted });
    }

    [HttpPost("reconcile")]
    [Consumes("application/xml")]
    public async Task<IActionResult> Reconcile(string accountId, CancellationToken ct)
    {
        using var buffer = new MemoryStream();
        await Request.Body.CopyToAsync(buffer, ct);
        buffer.Position = 0;
        var lines = _imports.ParseStatementXml(buffer);
        return Ok(new { account_id = accountId, lines = lines.Count, total = lines.Sum(l => l.Amount) });
    }
}
