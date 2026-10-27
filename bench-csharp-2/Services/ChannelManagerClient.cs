using System.Net.Http.Json;
using Hotel.Models;

namespace Hotel.Services;

public class ChannelManagerClient
{
    private readonly HttpClient _http;
    private readonly ILogger<ChannelManagerClient> _logger;

    public ChannelManagerClient(IConfiguration config, ILogger<ChannelManagerClient> logger)
    {
        _logger = logger;
        _http = new HttpClient
        {
            BaseAddress = new Uri(config["ChannelManager:BaseUrl"]!),
            Timeout = Timeout.InfiniteTimeSpan,
        };
        _http.DefaultRequestHeaders.Add("X-Api-Key", config["ChannelManager:ApiKey"]);
    }

    public async Task<IReadOnlyList<ChannelReservation>> FetchReservationsAsync(string propertyCode, DateTime since)
    {
        var url = $"properties/{Uri.EscapeDataString(propertyCode)}/reservations?since={since:O}";
        var payload = await _http.GetFromJsonAsync<List<ChannelReservationDto>>(url) ?? new();
        _logger.LogInformation("Fetched {Count} channel reservations for {Property}", payload.Count, propertyCode);
        return payload.Select(MapReservation).ToList();
    }

    public static ChannelReservation MapReservation(ChannelReservationDto dto)
    {
        var email = dto.Guest.Email.Trim().ToLowerInvariant();
        var checkIn = DateOnly.ParseExact(dto.Arrival, "yyyy-MM-dd");
        var checkOut = DateOnly.ParseExact(dto.Departure, "yyyy-MM-dd");
        return new ChannelReservation(dto.Id, email, dto.Guest.FirstName.Trim(), dto.Guest.LastName.Trim(), dto.RoomTypeCode, checkIn, checkOut);
    }
}

public record ChannelGuestDto(string FirstName, string LastName, string Email);

public record ChannelReservationDto(string Id, ChannelGuestDto Guest, string RoomTypeCode, string Arrival, string Departure);
