using Hotel.Data;
using Hotel.Models;
using Hotel.Security;
using Hotel.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Hotel.Controllers;

[ApiController]
[Authorize]
[Route("api/bookings")]
public class BookingsController : ControllerBase
{
    private readonly HotelDbContext _db;
    private readonly BookingRepository _repository;
    private readonly BookingService _bookings;
    private readonly RoomInventoryService _inventory;

    public BookingsController(HotelDbContext db, BookingRepository repository, BookingService bookings, RoomInventoryService inventory)
    {
        _db = db;
        _repository = repository;
        _bookings = bookings;
        _inventory = inventory;
    }

    [HttpPost]
    public async Task<IActionResult> Create(CreateBookingRequest request, CancellationToken ct)
    {
        var hotelId = User.GetHotelId();
        var available = await _inventory.CountAvailableAsync(hotelId, request.RoomType, request.CheckIn, request.CheckOut, ct);
        if (available == 0)
        {
            return Conflict(new { error = "No rooms available for those dates." });
        }
        var booking = await _bookings.CreateAsync(hotelId, request, ct);
        return CreatedAtAction(nameof(GetSummary), new { code = booking.ConfirmationCode }, new { booking.Id, booking.ConfirmationCode, booking.TotalAmount });
    }

    [HttpGet]
    public async Task<IActionResult> Search([FromQuery] BookingSearchRequest request, CancellationToken ct)
    {
        var hotelId = User.GetHotelId();
        var items = await _repository.SearchAsync(hotelId, request.Page, request.PageSize, request.ArrivingOn, ct);
        var total = await _repository.CountAsync(hotelId, request.ArrivingOn, ct);
        return Ok(new { items, total, request.Page, request.PageSize });
    }

    [HttpGet("{code}/summary")]
    public async Task<IActionResult> GetSummary(string code, CancellationToken ct)
    {
        return Ok(new { summary = await _bookings.GetConfirmationSummaryAsync(User.GetHotelId(), code, ct) });
    }

    [HttpGet("arrivals")]
    public async Task<IActionResult> Arrivals([FromQuery] DateOnly day, CancellationToken ct)
    {
        var arrivals = await _repository.GetArrivalsAsync(User.GetHotelId(), day, ct);
        return Ok(arrivals.Select(BookingService.BuildArrivalCard));
    }

    [HttpPost("{id:int}/cancel")]
    public async Task<IActionResult> Cancel(int id, CancellationToken ct)
    {
        return await _bookings.CancelAsync(User.GetHotelId(), id, ct) ? NoContent() : NotFound();
    }

    [HttpPut("{id:int}/dates")]
    public async Task<IActionResult> ModifyDates(int id, ModifyDatesRequest request, CancellationToken ct)
    {
        if (request.CheckOut <= request.CheckIn)
        {
            return BadRequest(new { error = "Check-out must be after check-in." });
        }

        await using var tx = await _db.Database.BeginTransactionAsync(ct);
        var booking = await _bookings.ModifyDatesAsync(User.GetHotelId(), id, request, ct);
        if (booking is null)
        {
            return NotFound();
        }
        await tx.CommitAsync(ct);
        return Ok(new { booking.Id, booking.CheckIn, booking.CheckOut, booking.TotalAmount });
    }

    [HttpPost("{id:int}/room/{roomId:int}")]
    public async Task<IActionResult> AssignRoom(int id, int roomId, CancellationToken ct)
    {
        return await _inventory.AssignRoomAsync(User.GetHotelId(), id, roomId, ct)
            ? NoContent()
            : Conflict(new { error = "Room is not available for this stay." });
    }

    [HttpPost("{id:int}/check-in")]
    public async Task<IActionResult> CheckIn(int id, CancellationToken ct)
    {
        return await _bookings.CheckInAsync(User.GetHotelId(), id, ct) ? NoContent() : Conflict();
    }

    [HttpPost("{id:int}/check-out")]
    public async Task<IActionResult> CheckOut(int id, CancellationToken ct)
    {
        return await _bookings.CheckOutAsync(User.GetHotelId(), id, ct) ? NoContent() : Conflict();
    }
}
