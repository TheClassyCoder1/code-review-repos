using System.Security.Claims;
using LedgerApi.Models;
using LedgerApi.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace LedgerApi.Controllers;

[ApiController]
[Authorize]
[Route("api/v1/accounts")]
public sealed class AccountsController : ControllerBase
{
    private readonly LedgerRepository _ledger;
    private readonly FxRateClient _fx;
    private readonly TransferService _transfers;
    private readonly AuditPublisher _audit;

    public AccountsController(LedgerRepository ledger, FxRateClient fx, TransferService transfers, AuditPublisher audit)
    {
        _ledger = ledger;
        _fx = fx;
        _transfers = transfers;
        _audit = audit;
    }

    [HttpGet("{accountId}")]
    [Authorize(Policy = "AccountOwner")]
    public async Task<ActionResult<Account>> GetAccount(string accountId, CancellationToken ct)
    {
        var account = await _ledger.GetAccountAsync(accountId, ct);
        if (account is null)
        {
            return NotFound();
        }
        return Ok(account);
    }

    [HttpGet("{accountId}/entries")]
    [Authorize(Policy = "AccountOwner")]
    public async Task<ActionResult<IReadOnlyList<LedgerEntry>>> ListEntries(
        string accountId, [FromQuery] EntrySort sort = EntrySort.Posted, [FromQuery] int limit = 50, CancellationToken ct = default)
    {
        limit = Math.Clamp(limit, 1, 500);
        return Ok(await _ledger.ListEntriesAsync(accountId, SortColumns.For(sort), limit, ct));
    }

    [HttpGet("{accountId}/entries/search")]
    [Authorize(Policy = "AccountOwner")]
    public async Task<ActionResult<IReadOnlyList<LedgerEntry>>> SearchEntries(string accountId, [FromQuery] string q, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(q))
        {
            return BadRequest("q is required");
        }
        return Ok(await _ledger.SearchEntriesAsync(accountId, q.Trim(), ct));
    }

    [HttpPut("{accountId}/limit")]
    public async Task<IActionResult> UpdateLimit(string accountId, [FromBody] UpdateLimitRequest request, CancellationToken ct)
    {
        if (request.DailyLimit < 0m || request.DailyLimit > 1_000_000m)
        {
            return BadRequest("daily limit out of range");
        }
        var updated = await _ledger.UpdateDailyLimitAsync(accountId, request.DailyLimit, ct);
        return updated ? NoContent() : NotFound();
    }

    [HttpGet("{accountId}/balance/{currency}")]
    [Authorize(Policy = "AccountOwner")]
    public async Task<IActionResult> GetBalanceIn(string accountId, string currency, CancellationToken ct)
    {
        var account = await _ledger.GetAccountAsync(accountId, ct);
        if (account is null)
        {
            return NotFound();
        }
        var rate = await _fx.GetRateAsync(account.Currency, currency.ToUpperInvariant(), ct);
        var converted = account.Balance * rate;
        return Ok(new { currency, amount = converted, amount_minor = FxRateClient.ToMinorUnits(converted, currency.ToUpperInvariant()) });
    }

    [HttpPost("{accountId}/transfers")]
    [Authorize(Policy = "AccountOwner")]
    public async Task<IActionResult> Transfer(string accountId, [FromBody] TransferRequest request, CancellationToken ct)
    {
        if (request.Amount <= 0m)
        {
            return BadRequest("amount must be positive");
        }
        var transferId = await _transfers.SubmitAsync(accountId, request.Amount, request.Currency, ct);
        _transfers.NotifyBalanceChanged(accountId);
        return Accepted(new { transfer_id = transferId });
    }

    [HttpPost("{accountId}/close")]
    [Authorize(Policy = "AccountOwner")]
    public async Task<IActionResult> Close(string accountId, CancellationToken ct)
    {
        var closed = await _ledger.CloseAccountAsync(accountId, ct);
        if (!closed)
        {
            return Conflict("account has a non-zero balance or is already closed");
        }
        var actor = User.FindFirstValue("sub") ?? "unknown";
        _ = _audit.EnqueueAsync(new AuditEvent("account.closed", accountId, actor, DateTime.UtcNow));
        return NoContent();
    }
}
