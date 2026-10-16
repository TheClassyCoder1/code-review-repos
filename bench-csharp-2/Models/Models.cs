using System.ComponentModel.DataAnnotations;

namespace Hotel.Models;

public enum BookingStatus { Held, Confirmed, CheckedIn, CheckedOut, Cancelled }

public enum RoomStatus { Clean, Dirty, Inspected, OutOfOrder }

public class Guest
{
    public int Id { get; set; }
    public int HotelId { get; set; }
    public string FirstName { get; set; } = "";
    public string LastName { get; set; } = "";
    public string Email { get; set; } = "";
    public string? Phone { get; set; }
    public string? PinDigest { get; set; }
}

public class Room
{
    public int Id { get; set; }
    public int HotelId { get; set; }
    public string Number { get; set; } = "";
    public string RoomType { get; set; } = "";
    public RoomStatus Status { get; set; }
    public DateTime? HousekeepingNotifiedAt { get; set; }
    public uint Version { get; set; }
}

public class RatePlan
{
    public int Id { get; set; }
    public int HotelId { get; set; }
    public string Code { get; set; } = "";
    public string RoomType { get; set; } = "";
    public decimal NightlyRate { get; set; }
    public decimal TaxPercent { get; set; }
    public bool Refundable { get; set; }
}

public class Booking
{
    public int Id { get; set; }
    public int HotelId { get; set; }
    public string ConfirmationCode { get; set; } = "";
    public int GuestId { get; set; }
    public Guest Guest { get; set; } = null!;
    public int? RoomId { get; set; }
    public Room? Room { get; set; }
    public int RatePlanId { get; set; }
    public RatePlan RatePlan { get; set; } = null!;
    public DateOnly CheckIn { get; set; }
    public DateOnly CheckOut { get; set; }
    public BookingStatus Status { get; set; }
    public decimal TotalAmount { get; set; }
    public DateTime CreatedAt { get; set; }
}

public class Folio
{
    public int Id { get; set; }
    public int BookingId { get; set; }
    public Booking Booking { get; set; } = null!;
    public decimal Balance { get; set; }
    public string Currency { get; set; } = "USD";
    public bool Closed { get; set; }
    public uint Version { get; set; }
    public List<FolioLine> Lines { get; set; } = new();
}

public class FolioLine
{
    public int Id { get; set; }
    public int FolioId { get; set; }
    public string Description { get; set; } = "";
    public decimal Amount { get; set; }
    public DateTime PostedAt { get; set; }
}

public static class DemandFactors
{
    public const double Low = 0.9;
    public const double Normal = 1.0;
    public const double High = 1.25;
    public const double Peak = 1.5;

    public static double ForOccupancy(int occupancyPercent)
    {
        return occupancyPercent switch
        {
            >= 95 => Peak,
            >= 80 => High,
            >= 40 => Normal,
            _ => Low,
        };
    }
}

public record CreateBookingRequest(
    [Required] int GuestId,
    [Required] int RatePlanId,
    [Required] string RoomType,
    DateOnly CheckIn,
    DateOnly CheckOut);

public record ModifyDatesRequest(DateOnly CheckIn, DateOnly CheckOut);

public record BookingSearchRequest
{
    [Range(1, 10_000)]
    public int Page { get; init; } = 1;

    [Range(1, 200)]
    public int PageSize { get; init; } = 50;

    public DateOnly? ArrivingOn { get; init; }
}

public record PostChargeRequest([Required] string Description, decimal Amount);

public record CreateRatePlanRequest([Required] string Code, [Required] string RoomType, decimal NightlyRate, decimal TaxPercent, bool Refundable);

public record FolioStatement(int FolioId, string Currency, decimal Balance, decimal BalanceInHomeCurrency, IReadOnlyList<FolioLine> Lines);

public record ChannelReservation(string ExternalId, string Email, string FirstName, string LastName, string RoomType, DateOnly CheckIn, DateOnly CheckOut);
