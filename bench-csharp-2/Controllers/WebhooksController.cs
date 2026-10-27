using System.Globalization;
using Hotel.Models;
using Hotel.Security;
using Hotel.Services;
using Microsoft.AspNetCore.Mvc;

namespace Hotel.Controllers;

[ApiController]
[Route("api/webhooks/channel-manager")]
public class WebhooksController : ControllerBase
{
    private readonly WebhookSignatureVerifier _verifier;
    private readonly GuestImportService _imports;
    private readonly ILogger<WebhooksController> _logger;

    public WebhooksController(WebhookSignatureVerifier verifier, GuestImportService imports, ILogger<WebhooksController> logger)
    {
        _verifier = verifier;
        _imports = imports;
        _logger = logger;
    }

    [HttpPost("{hotelId:int}/reservations")]
    public async Task<IActionResult> Reservation(int hotelId, CancellationToken ct)
    {
        var payload = await _imports.ReadPayloadAsync(Request.Body, ct);
        if (!long.TryParse(Request.Headers["X-Channel-Timestamp"], NumberStyles.Integer, CultureInfo.InvariantCulture, out var timestamp)
            || !_verifier.IsValid(Request.Headers["X-Channel-Signature"], timestamp, payload))
        {
            return Unauthorized();
        }

        var dto = GuestImportService.DeserializeReservation(payload);
        if (dto is null)
        {
            return BadRequest();
        }

        var reservation = ChannelManagerClient.MapReservation(dto);
        var created = await _imports.UpsertGuestAsync(new Guest
        {
            HotelId = hotelId,
            Email = reservation.Email,
            FirstName = reservation.FirstName,
            LastName = reservation.LastName,
        }, ct);
        _logger.LogInformation("Channel reservation {ExternalId} received for hotel {HotelId}", reservation.ExternalId, hotelId);
        return Ok(new { received = reservation.ExternalId, newGuest = created });
    }
}
