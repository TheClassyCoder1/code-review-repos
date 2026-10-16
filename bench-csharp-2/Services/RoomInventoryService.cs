using Hotel.Data;
using Hotel.Models;
using Microsoft.EntityFrameworkCore;

namespace Hotel.Services;

public class RoomInventoryService
{
    private readonly HotelDbContext _db;
    private readonly HousekeepingNotifier _housekeeping;

    public RoomInventoryService(HotelDbContext db, HousekeepingNotifier housekeeping)
    {
        _db = db;
        _housekeeping = housekeeping;
    }

    public async Task<bool> AssignRoomAsync(int hotelId, int bookingId, int roomId, CancellationToken ct)
    {
        var booking = await _db.Bookings.SingleOrDefaultAsync(b => b.Id == bookingId && b.HotelId == hotelId, ct);
        var room = await _db.Rooms.AsNoTracking().SingleOrDefaultAsync(r => r.Id == roomId && r.HotelId == hotelId, ct);
        if (booking is null || room is null || room.Status == RoomStatus.OutOfOrder)
        {
            return false;
        }

        var occupied = await _db.Bookings.AnyAsync(b =>
            b.RoomId == roomId && b.Id != bookingId &&
            b.Status != BookingStatus.Cancelled && b.Status != BookingStatus.CheckedOut &&
            b.CheckIn < booking.CheckOut && booking.CheckIn < b.CheckOut, ct);
        if (occupied)
        {
            return false;
        }

        booking.RoomId = roomId;
        await _db.SaveChangesAsync(ct);
        return true;
    }

    public async Task<IReadOnlyList<Room>> ListRoomsAsync(int hotelId, RoomStatus? status, CancellationToken ct)
    {
        var query = _db.Rooms.AsNoTracking().Where(r => r.HotelId == hotelId);
        if (status is { } s)
        {
            query = query.Where(r => r.Status == s);
        }
        return await query.OrderBy(r => r.Number).ToListAsync(ct);
    }

    public async Task<bool> UpdateRoomStatusAsync(int hotelId, int roomId, RoomStatus next, CancellationToken ct)
    {
        var room = await _db.Rooms.SingleOrDefaultAsync(r => r.Id == roomId && r.HotelId == hotelId, ct);
        if (room is null || !IsAllowedTransition(room.Status, next))
        {
            return false;
        }

        room.Status = next;
        if (next == RoomStatus.Dirty)
        {
            room.HousekeepingNotifiedAt = null;
        }
        await _db.SaveChangesAsync(ct);

        if (next == RoomStatus.Dirty)
        {
            await _housekeeping.NotifyRoomDirtyAsync(room, ct);
        }
        return true;
    }

    private static bool IsAllowedTransition(RoomStatus current, RoomStatus next)
    {
        return (current, next) switch
        {
            (RoomStatus.Dirty, RoomStatus.Clean) => true,
            (RoomStatus.Clean, RoomStatus.Inspected) => true,
            (RoomStatus.Inspected, RoomStatus.Dirty) => true,
            (RoomStatus.Clean, RoomStatus.Dirty) => true,
            (_, RoomStatus.OutOfOrder) => true,
            (RoomStatus.OutOfOrder, RoomStatus.Dirty) => true,
            _ => false,
        };
    }

    public async Task<int> CountAvailableAsync(int hotelId, string roomType, DateOnly checkIn, DateOnly checkOut, CancellationToken ct)
    {
        var total = await _db.Rooms.CountAsync(r =>
            r.HotelId == hotelId && r.RoomType == roomType && r.Status != RoomStatus.OutOfOrder, ct);
        var booked = await _db.Bookings.CountAsync(b =>
            b.HotelId == hotelId && b.RatePlan.RoomType == roomType &&
            (b.Status == BookingStatus.Confirmed || b.Status == BookingStatus.CheckedIn) &&
            b.CheckIn < checkOut && checkIn < b.CheckOut, ct);
        return Math.Max(0, total - booked);
    }
}
