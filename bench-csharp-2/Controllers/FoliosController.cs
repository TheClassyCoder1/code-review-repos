using Hotel.Data;
using Hotel.Models;
using Hotel.Security;
using Hotel.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Hotel.Controllers;

[ApiController]
[Authorize]
[Route("api/folios")]
public class FoliosController : ControllerBase
{
    private readonly HotelDbContext _db;
    private readonly FolioService _folios;
    private readonly FxRateClient _fx;
    private readonly GuestPinVerifier _pins;
    private readonly IConfiguration _config;

    public FoliosController(HotelDbContext db, FolioService folios, FxRateClient fx, GuestPinVerifier pins, IConfiguration config)
    {
        _db = db;
        _folios = folios;
        _fx = fx;
        _pins = pins;
        _config = config;
    }

    [HttpGet("{id:int}/statement")]
    public async Task<IActionResult> Statement(int id, CancellationToken ct)
    {
        var folio = await _folios.GetFolioWithLinesAsync(User.GetHotelId(), id, ct);
        if (folio is null)
        {
            return NotFound();
        }

        var homeCurrency = _config["Hotel:HomeCurrency"] ?? "USD";
        var folioTask = Task.FromResult(folio);
        var fxTask = _fx.GetRateAsync(folio.Currency, homeCurrency, ct);
        await Task.WhenAll(folioTask, fxTask);
        return Ok(FolioService.ComposeStatement(folioTask, fxTask));
    }

    [HttpGet("{id:int}/invoice")]
    public IActionResult Invoice(int id)
    {
        var stream = _folios.OpenInvoicePdf(User.GetHotelId(), id);
        if (stream is null)
        {
            return NotFound();
        }
        return File(stream, "application/pdf", $"invoice-{id}.pdf");
    }

    [HttpPost("{id:int}/charges")]
    public async Task<IActionResult> PostCharge(int id, PostChargeRequest request, CancellationToken ct)
    {
        if (!await _folios.BelongsToHotelAsync(User.GetHotelId(), id, ct))
        {
            return NotFound();
        }
        var balance = await _folios.PostChargeAsync(id, request, ct);
        return Ok(new { balance });
    }

    [HttpPost("{id:int}/settle")]
    public async Task<IActionResult> Settle(int id, [FromBody] string paymentToken, CancellationToken ct)
    {
        if (!await _folios.BelongsToHotelAsync(User.GetHotelId(), id, ct))
        {
            return NotFound();
        }
        var settled = await _folios.SettleAsync(id, paymentToken, ct);
        return settled ? Ok(new { status = "settled" }) : Problem("Settlement failed.");
    }

    [AllowAnonymous]
    [HttpPost("guest/{guestId:int}/verify-pin")]
    public async Task<IActionResult> VerifyPin(int guestId, [FromBody] string pin, CancellationToken ct)
    {
        var guest = await _db.Guests.AsNoTracking().SingleOrDefaultAsync(g => g.Id == guestId, ct);
        if (guest is null || !_pins.Verify(guest, pin))
        {
            return Unauthorized();
        }
        return Ok(new { verified = true });
    }
}
