using Hotel.Models;
using Hotel.Security;
using Hotel.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Hotel.Controllers;

[ApiController]
[Authorize(Roles = "RevenueManager")]
[Route("api/rate-plans")]
public class RatePlansController : ControllerBase
{
    private readonly RatePlanService _rates;

    public RatePlansController(RatePlanService rates)
    {
        _rates = rates;
    }

    [HttpGet]
    public async Task<IActionResult> List([FromQuery] string? roomType, CancellationToken ct)
    {
        return Ok(await _rates.ListPlansAsync(User.GetHotelId(), roomType, ct));
    }

    [HttpPost]
    public async Task<IActionResult> Create(CreateRatePlanRequest request, CancellationToken ct)
    {
        var plan = await _rates.CreatePlanAsync(User.GetHotelId(), request, ct);
        return plan is null ? Conflict(new { error = "Rate plan code already exists." }) : Ok(plan);
    }

    [HttpGet("{id:int}/quote")]
    public async Task<IActionResult> Quote(int id, [FromQuery] DateOnly checkIn, [FromQuery] DateOnly checkOut, CancellationToken ct)
    {
        var hotelId = User.GetHotelId();
        var plan = await _rates.GetPlanAsync(hotelId, id, ct);
        if (plan is null)
        {
            return NotFound();
        }

        var occupancy = await _rates.OccupancyPercentAsync(hotelId, checkIn, ct);
        var nightly = RatePlanService.ApplyDemandFactor(plan.NightlyRate, DemandFactors.ForOccupancy(occupancy));
        var nights = RatePlanService.CountNights(checkIn, checkOut);
        var total = RatePlanService.ApplyLengthOfStayDiscount(nightly * nights, nights);
        return Ok(new { plan.Code, nightly, nights, total });
    }
}
