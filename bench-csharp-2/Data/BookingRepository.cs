using Hotel.Models;
using Microsoft.EntityFrameworkCore;

namespace Hotel.Data;

public class BookingRepository
{
    private readonly HotelDbContext _db;

    public BookingRepository(HotelDbContext db)
    {
        _db = db;
    }

    public async Task<IReadOnlyList<Booking>> SearchAsync(int hotelId, int page, int pageSize, DateOnly? arrivingOn, CancellationToken ct)
    {
        var query = _db.Bookings.AsNoTracking().Where(b => b.HotelId == hotelId);
        if (arrivingOn is { } day)
        {
            query = query.Where(b => b.CheckIn == day);
        }
        return await query
            .OrderBy(b => b.CheckIn).ThenBy(b => b.Id)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync(ct);
    }

    public async Task<int> CountAsync(int hotelId, DateOnly? arrivingOn, CancellationToken ct)
    {
        var query = _db.Bookings.Where(b => b.HotelId == hotelId);
        if (arrivingOn is { } day)
        {
            query = query.Where(b => b.CheckIn == day);
        }
        return await query.CountAsync(ct);
    }

    public async Task<IReadOnlyList<Booking>> GetArrivalsAsync(int hotelId, DateOnly day, CancellationToken ct)
    {
        return await _db.Bookings
            .AsNoTracking()
            .Include(b => b.Guest)
            .Include(b => b.Room)
            .Where(b => b.HotelId == hotelId && b.CheckIn == day && b.Status == BookingStatus.Confirmed)
            .OrderBy(b => b.Guest.LastName)
            .ToListAsync(ct);
    }

    public async Task<IReadOnlyList<Booking>> GetDeparturesAsync(int hotelId, DateOnly day, CancellationToken ct)
    {
        return await _db.Bookings
            .AsNoTracking()
            .Where(b => b.HotelId == hotelId && b.CheckOut == day && b.Status == BookingStatus.CheckedIn)
            .ToListAsync(ct);
    }

    public async Task<Booking?> FindByConfirmationAsync(int hotelId, string confirmationCode, CancellationToken ct)
    {
        var normalized = confirmationCode.Trim().ToUpperInvariant();
        return await _db.Bookings
            .Include(b => b.RatePlan)
            .SingleOrDefaultAsync(b => b.HotelId == hotelId && b.ConfirmationCode == normalized, ct);
    }
}
