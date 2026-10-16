using Hotel.Models;
using Hotel.Security;
using Hotel.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Hotel.Controllers;

[ApiController]
[Authorize]
[Route("api/rooms")]
public class RoomsController : ControllerBase
{
    private readonly RoomInventoryService _inventory;

    public RoomsController(RoomInventoryService inventory)
    {
        _inventory = inventory;
    }

    [HttpGet]
    public async Task<IActionResult> List([FromQuery] RoomStatus? status, CancellationToken ct)
    {
        return Ok(await _inventory.ListRoomsAsync(User.GetHotelId(), status, ct));
    }

    [HttpPut("{id:int}/status/{next}")]
    public async Task<IActionResult> UpdateStatus(int id, RoomStatus next, CancellationToken ct)
    {
        try
        {
            return await _inventory.UpdateRoomStatusAsync(User.GetHotelId(), id, next, ct)
                ? NoContent()
                : Conflict(new { error = "Status change not allowed." });
        }
        catch (DbUpdateConcurrencyException)
        {
            return Conflict(new { error = "Room was updated by someone else; reload and retry." });
        }
    }

    [HttpGet("availability")]
    public async Task<IActionResult> Availability([FromQuery] string roomType, [FromQuery] DateOnly checkIn, [FromQuery] DateOnly checkOut, CancellationToken ct)
    {
        if (checkOut <= checkIn)
        {
            return BadRequest(new { error = "Check-out must be after check-in." });
        }
        var available = await _inventory.CountAvailableAsync(User.GetHotelId(), roomType, checkIn, checkOut, ct);
        return Ok(new { roomType, available });
    }
}
