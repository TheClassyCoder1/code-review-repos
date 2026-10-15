using System.Text;
using Hotel.Security;
using Hotel.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Hotel.Controllers;

[ApiController]
[Authorize(Roles = "NightAuditor")]
[Route("api/admin")]
public class AdminController : ControllerBase
{
    private readonly NightAuditService _audit;
    private readonly GuestImportService _imports;
    private readonly ChannelManagerClient _channels;
    private readonly IConfiguration _config;

    public AdminController(NightAuditService audit, GuestImportService imports, ChannelManagerClient channels, IConfiguration config)
    {
        _audit = audit;
        _imports = imports;
        _channels = channels;
        _config = config;
    }

    [HttpGet("exports/bookings.csv")]
    public async Task<IActionResult> ExportBookings(CancellationToken ct)
    {
        var csv = await _audit.ExportBookingsCsvAsync(User.GetHotelId(), ct);
        return File(Encoding.UTF8.GetBytes(csv), "text/csv", "bookings.csv");
    }

    [HttpPost("housekeeping/resend")]
    public async Task<IActionResult> ResendHousekeeping(CancellationToken ct)
    {
        return Ok(new { sent = await _audit.ResendPendingHousekeepingAsync(User.GetHotelId(), ct) });
    }

    [HttpPost("imports/guests")]
    public async Task<IActionResult> ImportGuests(CancellationToken ct)
    {
        var hotelId = User.GetHotelId();
        var dir = _config["Imports:Dir"] ?? "/var/lib/hotel/imports";
        var path = Path.Combine(dir, $"guests-{hotelId}.csv");
        if (!System.IO.File.Exists(path))
        {
            return NotFound();
        }
        return Ok(new { imported = await _imports.ImportFromFileAsync(hotelId, path, ct) });
    }

    [HttpPost("channels/{propertyCode}/sync")]
    public async Task<IActionResult> SyncChannel(string propertyCode, [FromQuery] DateTime since)
    {
        var reservations = await _channels.FetchReservationsAsync(propertyCode, since);
        return Ok(new { count = reservations.Count });
    }
}
