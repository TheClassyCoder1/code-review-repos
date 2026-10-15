using Hotel.Data;
using Hotel.Models;

namespace Hotel.Services;

public class HousekeepingNotifier
{
    private readonly HotelDbContext _db;
    private readonly IEmailSender _email;
    private readonly IConfiguration _config;
    private readonly ILogger<HousekeepingNotifier> _logger;

    public HousekeepingNotifier(HotelDbContext db, IEmailSender email, IConfiguration config, ILogger<HousekeepingNotifier> logger)
    {
        _db = db;
        _email = email;
        _config = config;
        _logger = logger;
    }

    public async Task NotifyRoomDirtyAsync(Room room, CancellationToken ct)
    {
        try
        {
            var inbox = _config["Housekeeping:Inbox"] ?? "housekeeping@hotel.example";
            await _email.SendAsync(inbox, $"Room {room.Number} needs cleaning", FormatMessage(room), ct);
            room.HousekeepingNotifiedAt = DateTime.UtcNow;
            await _db.SaveChangesAsync(ct);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Housekeeping notification failed for room {RoomId}", room.Id);
        }
    }

    public async Task NotifyRoomsAsync(IEnumerable<Room> rooms, CancellationToken ct)
    {
        foreach (var room in rooms)
        {
            await NotifyRoomDirtyAsync(room, ct);
        }
    }

    public static string FormatMessage(Room room)
    {
        var priority = room.RoomType.StartsWith("SUITE", StringComparison.OrdinalIgnoreCase) ? "HIGH" : "NORMAL";
        var since = room.HousekeepingNotifiedAt?.ToString("u") ?? "now";
        return $"Room {room.Number} ({room.RoomType}) is dirty since {since}. Priority: {priority}.";
    }
}
