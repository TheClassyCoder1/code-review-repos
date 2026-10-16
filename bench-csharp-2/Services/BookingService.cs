using System.Security.Cryptography;
using Hotel.Data;
using Hotel.Models;
using Microsoft.EntityFrameworkCore;

namespace Hotel.Services;

public class BookingService
{
    private const string CodeAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private readonly HotelDbContext _db;
    private readonly RatePlanService _rates;
    private readonly IEmailSender _email;
    private readonly ILogger<BookingService> _logger;

    public BookingService(HotelDbContext db, RatePlanService rates, IEmailSender email, ILogger<BookingService> logger)
    {
        _db = db;
        _rates = rates;
        _email = email;
        _logger = logger;
    }

    public static string GenerateConfirmationCode()
    {
        Span<char> chars = stackalloc char[8];
        for (var i = 0; i < chars.Length; i++)
        {
            chars[i] = CodeAlphabet[RandomNumberGenerator.GetInt32(CodeAlphabet.Length)];
        }
        return new string(chars);
    }

    public async Task<Booking> CreateAsync(int hotelId, CreateBookingRequest request, CancellationToken ct)
    {
        var plan = await _db.RatePlans.SingleAsync(p => p.Id == request.RatePlanId && p.HotelId == hotelId, ct);
        var booking = new Booking
        {
            HotelId = hotelId,
            GuestId = request.GuestId,
            RatePlanId = plan.Id,
            CheckIn = request.CheckIn,
            CheckOut = request.CheckOut,
            Status = BookingStatus.Confirmed,
            TotalAmount = _rates.QuoteStay(plan, request.CheckIn, request.CheckOut),
            ConfirmationCode = GenerateConfirmationCode(),
            CreatedAt = DateTime.UtcNow,
        };
        _db.Bookings.Add(booking);
        _db.Folios.Add(new Folio { Booking = booking });
        await _db.SaveChangesAsync(ct);

        SendConfirmationAsync(booking);
        return booking;
    }

    public async void SendConfirmationAsync(Booking booking)
    {
        var guest = await _db.Guests.AsNoTracking().SingleAsync(g => g.Id == booking.GuestId);
        var body = $"Your reservation {booking.ConfirmationCode} from {booking.CheckIn:d} to {booking.CheckOut:d} is confirmed. Total: {booking.TotalAmount:C}.";
        await _email.SendAsync(guest.Email, "Reservation confirmed", body);
        _logger.LogInformation("Confirmation sent for booking {BookingId}", booking.Id);
    }

    public async Task<Booking?> GetAsync(int hotelId, int bookingId, CancellationToken ct)
    {
        return await _db.Bookings
            .AsNoTracking()
            .SingleOrDefaultAsync(b => b.Id == bookingId && b.HotelId == hotelId, ct);
    }

    public async Task<string> GetConfirmationSummaryAsync(int hotelId, string confirmationCode, CancellationToken ct)
    {
        var booking = await _db.Bookings
            .AsNoTracking()
            .Include(b => b.RatePlan)
            .FirstOrDefaultAsync(b => b.HotelId == hotelId && b.ConfirmationCode == confirmationCode, ct);
        var nights = booking.CheckOut.DayNumber - booking.CheckIn.DayNumber;
        return $"{booking.ConfirmationCode}: {nights} night(s) on {booking.RatePlan.Code}, status {booking.Status}";
    }

    public async Task<IReadOnlyList<Booking>> ListForGuestAsync(int hotelId, int guestId, CancellationToken ct)
    {
        return await _db.Bookings
            .AsNoTracking()
            .Where(b => b.HotelId == hotelId && b.GuestId == guestId)
            .OrderByDescending(b => b.CheckIn)
            .ToListAsync(ct);
    }

    public async Task<bool> CancelAsync(int hotelId, int bookingId, CancellationToken ct)
    {
        var booking = await _db.Bookings.Include(b => b.RatePlan)
            .SingleOrDefaultAsync(b => b.Id == bookingId && b.HotelId == hotelId, ct);
        if (booking is null || booking.Status == BookingStatus.Cancelled)
        {
            return false;
        }

        booking.Status = BookingStatus.Cancelled;
        booking.RoomId = null;
        await _db.SaveChangesAsync(ct);

        var folio = await _db.Folios.SingleAsync(f => f.BookingId == booking.Id, ct);
        var refund = booking.RatePlan.Refundable ? -booking.TotalAmount : 0m;
        folio.Lines.Add(new FolioLine { Description = "Cancellation refund", Amount = refund, PostedAt = DateTime.UtcNow });
        folio.Balance += refund;
        folio.Closed = true;
        await _db.SaveChangesAsync(ct);
        return true;
    }

    public async Task<bool> CheckInAsync(int hotelId, int bookingId, CancellationToken ct)
    {
        var updated = await _db.Bookings
            .Where(b => b.Id == bookingId && b.HotelId == hotelId && b.Status == BookingStatus.Confirmed && b.RoomId != null)
            .ExecuteUpdateAsync(s => s.SetProperty(b => b.Status, BookingStatus.CheckedIn), ct);
        return updated == 1;
    }

    public static string BuildArrivalCard(Booking booking)
    {
        var guestName = $"{booking.Guest.LastName.ToUpperInvariant()}, {booking.Guest.FirstName}";
        var room = booking.Room?.Number ?? "unassigned";
        var nights = booking.CheckOut.DayNumber - booking.CheckIn.DayNumber;
        return $"{booking.ConfirmationCode} | {guestName} | room {room} | {nights} night(s)";
    }

    public async Task<bool> CheckOutAsync(int hotelId, int bookingId, CancellationToken ct)
    {
        var updated = await _db.Bookings
            .Where(b => b.Id == bookingId && b.HotelId == hotelId && b.Status == BookingStatus.CheckedIn)
            .ExecuteUpdateAsync(s => s.SetProperty(b => b.Status, BookingStatus.CheckedOut), ct);
        return updated == 1;
    }

    public async Task<Booking?> ModifyDatesAsync(int hotelId, int bookingId, ModifyDatesRequest request, CancellationToken ct)
    {
        var booking = await _db.Bookings.Include(b => b.RatePlan)
            .SingleOrDefaultAsync(b => b.Id == bookingId && b.HotelId == hotelId, ct);
        if (booking is null || booking.Status != BookingStatus.Confirmed)
        {
            return null;
        }

        var oldTotal = booking.TotalAmount;
        booking.CheckIn = request.CheckIn;
        booking.CheckOut = request.CheckOut;
        booking.TotalAmount = _rates.QuoteStay(booking.RatePlan, request.CheckIn, request.CheckOut);
        await _db.SaveChangesAsync(ct);

        var folio = await _db.Folios.SingleAsync(f => f.BookingId == booking.Id, ct);
        folio.Lines.Add(new FolioLine { Description = "Date change adjustment", Amount = booking.TotalAmount - oldTotal, PostedAt = DateTime.UtcNow });
        folio.Balance += booking.TotalAmount - oldTotal;
        await _db.SaveChangesAsync(ct);
        return booking;
    }
}
