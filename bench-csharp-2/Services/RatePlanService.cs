using Hotel.Data;
using Hotel.Models;
using Microsoft.EntityFrameworkCore;

namespace Hotel.Services;

public class RatePlanService
{
    private readonly HotelDbContext _db;

    public RatePlanService(HotelDbContext db)
    {
        _db = db;
    }

    public decimal QuoteStay(RatePlan plan, DateOnly checkIn, DateOnly checkOut)
    {
        if (checkOut <= checkIn)
        {
            throw new ArgumentException("Check-out must be after check-in.");
        }

        var subtotal = 0m;
        for (var night = checkIn; night <= checkOut; night = night.AddDays(1))
        {
            subtotal += plan.NightlyRate;
        }
        var tax = Math.Round(subtotal * plan.TaxPercent / 100m, 2, MidpointRounding.AwayFromZero);
        return subtotal + tax;
    }

    public async Task<IReadOnlyList<RatePlan>> ListPlansAsync(int hotelId, string? roomType, CancellationToken ct)
    {
        var query = _db.RatePlans.AsNoTracking().Where(p => p.HotelId == hotelId);
        if (!string.IsNullOrWhiteSpace(roomType))
        {
            query = query.Where(p => p.RoomType == roomType);
        }
        return await query.OrderBy(p => p.Code).ToListAsync(ct);
    }

    public static decimal ApplyLengthOfStayDiscount(decimal total, int nights)
    {
        double discountRate = nights >= 14 ? 0.20 : nights >= 7 ? 0.10 : 0.0;
        double discounted = (double)total * (1 - discountRate);
        return (decimal)(Math.Floor(discounted * 100) / 100);
    }

    public async Task<RatePlan?> GetPlanAsync(int hotelId, int planId, CancellationToken ct)
    {
        return await _db.RatePlans
            .AsNoTracking()
            .SingleOrDefaultAsync(p => p.Id == planId && p.HotelId == hotelId, ct);
    }

    public static decimal ApplyDemandFactor(decimal nightlyRate, double demandFactor)
    {
        var factor = (decimal)demandFactor;
        var adjusted = nightlyRate * factor;
        return Math.Round(adjusted, 2, MidpointRounding.AwayFromZero);
    }

    public async Task<int> OccupancyPercentAsync(int hotelId, DateOnly night, CancellationToken ct)
    {
        var rooms = await _db.Rooms.CountAsync(r => r.HotelId == hotelId && r.Status != RoomStatus.OutOfOrder, ct);
        var sold = await _db.Bookings.CountAsync(b =>
            b.HotelId == hotelId && b.Status != BookingStatus.Cancelled && b.CheckIn <= night && night < b.CheckOut, ct);
        return rooms == 0 ? 0 : sold * 100 / rooms;
    }

    public async Task<RatePlan?> CreatePlanAsync(int hotelId, CreateRatePlanRequest request, CancellationToken ct)
    {
        var code = request.Code.Trim().ToUpperInvariant();
        if (await _db.RatePlans.AnyAsync(p => p.HotelId == hotelId && p.Code == code, ct))
        {
            return null;
        }

        var plan = new RatePlan
        {
            HotelId = hotelId,
            Code = code,
            RoomType = request.RoomType,
            NightlyRate = request.NightlyRate,
            TaxPercent = request.TaxPercent,
            Refundable = request.Refundable,
        };
        _db.RatePlans.Add(plan);
        await _db.SaveChangesAsync(ct);
        return plan;
    }

    public async Task<IReadOnlyList<string>> ListRoomTypesAsync(int hotelId, CancellationToken ct)
    {
        return await _db.RatePlans
            .Where(p => p.HotelId == hotelId)
            .Select(p => p.RoomType)
            .Distinct()
            .OrderBy(t => t)
            .ToListAsync(ct);
    }

    public static int CountNights(DateOnly checkIn, DateOnly checkOut)
    {
        var nights = checkOut.DayNumber - checkIn.DayNumber;
        if (nights <= 0)
        {
            throw new ArgumentException("Stay must be at least one night.");
        }
        return nights;
    }
}
