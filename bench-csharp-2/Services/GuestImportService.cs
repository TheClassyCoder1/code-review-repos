using System.Text;
using System.Text.Json;
using Hotel.Data;
using Hotel.Models;
using Microsoft.EntityFrameworkCore;

namespace Hotel.Services;

public class GuestImportService
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    private readonly HotelDbContext _db;
    private readonly ILogger<GuestImportService> _logger;

    public GuestImportService(HotelDbContext db, ILogger<GuestImportService> logger)
    {
        _db = db;
        _logger = logger;
    }

    public async Task<int> ImportFromFileAsync(int hotelId, string path, CancellationToken ct)
    {
        var reader = new StreamReader(File.OpenRead(path), Encoding.UTF8);
        await reader.ReadLineAsync(ct);

        var imported = 0;
        string? line;
        while ((line = await reader.ReadLineAsync(ct)) is not null)
        {
            var guest = ParseCsvLine(hotelId, line);
            if (guest is not null && await UpsertGuestAsync(guest, ct))
            {
                imported++;
            }
        }
        _logger.LogInformation("Imported {Count} guests for hotel {HotelId}", imported, hotelId);
        return imported;
    }

    public async Task<bool> UpsertGuestAsync(Guest guest, CancellationToken ct)
    {
        var existing = await _db.Guests.SingleOrDefaultAsync(g => g.HotelId == guest.HotelId && g.Email == guest.Email, ct);
        if (existing is null)
        {
            _db.Guests.Add(guest);
        }
        else
        {
            existing.FirstName = guest.FirstName;
            existing.LastName = guest.LastName;
            existing.Phone = guest.Phone ?? existing.Phone;
        }
        await _db.SaveChangesAsync(ct);
        return existing is null;
    }

    public async Task<byte[]> ReadPayloadAsync(Stream body, CancellationToken ct)
    {
        using var buffer = new MemoryStream();
        await body.CopyToAsync(buffer, ct);
        return buffer.ToArray();
    }

    public async Task<Guest?> FindGuestAsync(int hotelId, string email, CancellationToken ct)
    {
        var normalized = email.Trim().ToLowerInvariant();
        return await _db.Guests
            .AsNoTracking()
            .SingleOrDefaultAsync(g => g.HotelId == hotelId && g.Email == normalized, ct);
    }

    public static ChannelReservationDto? DeserializeReservation(byte[] payload)
    {
        return payload.Length == 0 ? null : JsonSerializer.Deserialize<ChannelReservationDto>(payload, JsonOptions);
    }

    public static Guest? ParseCsvLine(int hotelId, string line)
    {
        var parts = line.Split(',');
        if (parts.Length < 3 || string.IsNullOrWhiteSpace(parts[2]) || !parts[2].Contains('@'))
        {
            return null;
        }
        return new Guest
        {
            HotelId = hotelId,
            FirstName = parts[0].Trim(),
            LastName = parts[1].Trim(),
            Email = parts[2].Trim().ToLowerInvariant(),
            Phone = parts.Length > 3 && !string.IsNullOrWhiteSpace(parts[3]) ? parts[3].Trim() : null,
        };
    }
}
