using System.ComponentModel.DataAnnotations;
using GiftCards.Data;
using GiftCards.Models;
using GiftCards.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GiftCards.Controllers;

[ApiController]
[Authorize]
[Route("api/giftcards")]
public sealed class GiftCardsController : ControllerBase
{
    private readonly GiftCardService _cards;
    private readonly GiftCardRepository _repository;
    private readonly TransferService _transfers;
    private readonly VoucherQueue _vouchers;

    public GiftCardsController(GiftCardService cards, GiftCardRepository repository, TransferService transfers, VoucherQueue vouchers)
    {
        _cards = cards;
        _repository = repository;
        _transfers = transfers;
        _vouchers = vouchers;
    }

    [HttpGet("{code}/balance")]
    public async Task<ActionResult<BalanceView>> GetBalance(string code, CancellationToken ct)
    {
        var card = await _cards.FindAsync(code, ct);
        if (card is null || card.OwnerId != User.GetOwnerId())
        {
            return NotFound();
        }
        return GiftCardService.ToBalanceView(card);
    }

    [HttpGet("{code}/summary")]
    public async Task<ActionResult<string>> Summary(string code, CancellationToken ct) =>
        await _cards.DescribeAsync(code, User.GetOwnerId(), ct);

    [HttpPost("{code}/top-up")]
    public async Task<IActionResult> TopUp(string code, TopUpRequest request, CancellationToken ct)
    {
        var balance = await _cards.TopUpAsync(code, User.GetOwnerId(), request.Amount, ct);
        return balance is null ? NotFound() : Ok(new { balance });
    }

    [HttpPost("{code}/replace")]
    public async Task<IActionResult> ReplaceLost(string code, CancellationToken ct)
    {
        var card = await _cards.FindAsync(code, ct);
        if (card is null || card.OwnerId != User.GetOwnerId() || card.Status != CardStatus.Active)
        {
            return NotFound();
        }
        var newCardId = await _cards.ReplaceLostCardAsync(card, ct);
        return Ok(new { newCardId });
    }

    [HttpPost("{id:long}/cancel")]
    public async Task<IActionResult> Cancel(long id, CancellationToken ct)
    {
        var updated = await _cards.CancelAsync(id, User.GetOwnerId(), ct);
        if (updated == 0)
        {
            return NotFound();
        }
        return NoContent();
    }

    [HttpGet("programmes/{programmeId:int}")]
    [Authorize(Roles = "programme-admin")]
    public async Task<ActionResult<IReadOnlyList<BalanceView>>> ListProgramme(
        int programmeId,
        [FromQuery, Range(1, 10_000)] int page = 1,
        [FromQuery, Range(1, 100)] int pageSize = 25,
        CancellationToken ct = default)
    {
        var cards = await _cards.ListProgrammeCardsAsync(programmeId, page, pageSize, ct);
        return cards.Select(GiftCardService.ToBalanceView).ToList();
    }

    [HttpPost("batches")]
    [Authorize(Roles = "programme-admin")]
    public async Task<IActionResult> IssueBatch(IssueBatchRequest request, CancellationToken ct)
    {
        var batchId = await _repository.NextBatchIdAsync(ct);
        for (var i = 0; i < request.Quantity; i++)
        {
            await _vouchers.EnqueueAsync(
                new VoucherIssueRequest(batchId, request.ProgrammeId, request.FaceValue, request.Currency, User.GetOwnerId()), ct);
        }
        return Accepted(new { batchId });
    }

    [HttpGet("batches/{batchId:long}")]
    [Authorize(Roles = "programme-admin")]
    public async Task<ActionResult<IReadOnlyList<BalanceView>>> GetBatch(long batchId, CancellationToken ct)
    {
        var cards = await _repository.GetBatchCardsAsync(batchId, ct);
        return cards.Select(GiftCardService.ToBalanceView).ToList();
    }

    [HttpPost("merge")]
    public async Task<IActionResult> Merge(TransferRequest request, CancellationToken ct)
    {
        var (from, to) = await _transfers.LoadPairAsync(request, ct);
        var error = TransferService.ValidateTransfer(from, to, User.GetOwnerId());
        if (error is not null)
        {
            return BadRequest(new { error });
        }
        var balance = await _transfers.MergeCardsAsync(from!, to!, ct);
        return Ok(new { balance });
    }
}
