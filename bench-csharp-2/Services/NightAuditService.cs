using System.Globalization;
using System.Text;
using Hotel.Data;
using Hotel.Models;
using Microsoft.EntityFrameworkCore;

namespace Hotel.Services;

public class NightAuditService
{
    private readonly HotelDbContext _db;
    private readonly HousekeepingNotifier _housekeeping;

    public NightAuditService(HotelDbContext db, HousekeepingNotifier housekeeping)
    {
        _db = db;
        _housekeeping = housekeeping;
    }

    public async Task<string> ExportBookingsCsvAsync(int hotelId, CancellationToken ct)
    {
        var bookings = await _db.Bookings
            .AsNoTracking()
            .Include(b => b.Guest)
            .Where(b => b.HotelId == hotelId)
            .OrderBy(b => b.CreatedAt)
            .ToListAsync(ct);

        var csv = new StringBuilder("confirmation,guest,check_in,check_out,status,total\n");
        foreach (var b in bookings)
        {
            csv.Append(CultureInfo.InvariantCulture,
                $"{b.ConfirmationCode},{b.Guest.LastName},{b.CheckIn:yyyy-MM-dd},{b.CheckOut:yyyy-MM-dd},{b.Status},{b.TotalAmount:F2}\n");
        }
        return csv.ToString();
    }

    public async Task<int> CountInHouseAsync(int hotelId, CancellationToken ct)
    {
        return await _db.Bookings
            .AsNoTracking()
            .CountAsync(b => b.HotelId == hotelId && b.Status == BookingStatus.CheckedIn, ct);
    }

    public async Task<int> ResendPendingHousekeepingAsync(int hotelId, CancellationToken ct)
    {
        var pending = await _db.Rooms
            .Where(r => r.HotelId == hotelId && r.Status == RoomStatus.Dirty && r.HousekeepingNotifiedAt == null)
            .OrderBy(r => r.Number)
            .Take(100)
            .ToListAsync(ct);
        await _housekeeping.NotifyRoomsAsync(pending, ct);
        return pending.Count(r => r.HousekeepingNotifiedAt != null);
    }
}
