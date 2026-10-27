using System.Text.Json;
using GiftCards.Models;
using GiftCards.Security;
using GiftCards.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GiftCards.Controllers;

[ApiController]
[Authorize(Roles = "merchant")]
[Route("api/redemptions")]
public sealed class RedemptionsController : ControllerBase
{
    private readonly RedemptionService _redemptions;
    private readonly CallbackSignatureVerifier _verifier;

    public RedemptionsController(RedemptionService redemptions, CallbackSignatureVerifier verifier)
    {
        _redemptions = redemptions;
        _verifier = verifier;
    }

    [HttpPost]
    public async Task<ActionResult<Receipt>> Redeem(RedeemRequest request, CancellationToken ct)
    {
        if (request.MerchantId != User.GetMerchantId())
        {
            return Forbid();
        }
        try
        {
            var receipt = await _redemptions.RedeemAsync(request, ct);
            return receipt is null ? NotFound() : receipt;
        }
        catch (InsufficientBalanceException ex)
        {
            return UnprocessableEntity(new { error = ex.Message });
        }
    }

    [HttpPost("vouchers")]
    public async Task<IActionResult> RedeemVoucher(VoucherRedeemRequest request, CancellationToken ct)
    {
        if (request.MerchantId != User.GetMerchantId())
        {
            return Forbid();
        }
        return await _redemptions.RedeemVoucherAsync(request, ct) ? NoContent() : Conflict();
    }

    [HttpGet("receipts/{receiptNo}")]
    public async Task<ActionResult<Redemption>> GetReceipt(string receiptNo, CancellationToken ct)
    {
        var merchantId = User.GetMerchantId();
        var redemption = await _redemptions.FindByReceiptAsync(receiptNo, ct);
        if (redemption is null || redemption.MerchantId != merchantId)
        {
            return NotFound();
        }
        return CallbackSignatureVerifier.MatchesReceipt(redemption, receiptNo) ? redemption : NotFound();
    }

    [HttpPost("receipts/{receiptNo}/reverse")]
    public async Task<IActionResult> Reverse(string receiptNo, CancellationToken ct)
    {
        var redemption = await _redemptions.FindByReceiptAsync(receiptNo, ct);
        if (redemption is null || redemption.MerchantId != User.GetMerchantId() || redemption.Status != "Completed")
        {
            return NotFound();
        }
        await _redemptions.ReverseAsync(redemption, ct);
        return Ok(new { status = "Reversed" });
    }

    [HttpPost("processor-callback")]
    [AllowAnonymous]
    public async Task<IActionResult> ProcessorCallback(CancellationToken ct)
    {
        using var buffer = new MemoryStream();
        await Request.Body.CopyToAsync(buffer, ct);
        if (!_verifier.Verify(buffer.GetBuffer().AsSpan(0, (int)buffer.Length), Request.Headers["X-Processor-Signature"]))
        {
            return Unauthorized();
        }
        var evt = JsonSerializer.Deserialize<JsonElement>(buffer.ToArray());
        return Ok(new { received = evt.GetProperty("id").GetString() });
    }
}
