using System.Security.Claims;

namespace Hotel.Security;

public static class ClaimsExtensions
{
    public static int GetHotelId(this ClaimsPrincipal user)
    {
        var value = user.FindFirstValue("hotel_id")
            ?? throw new UnauthorizedAccessException("hotel_id claim missing");
        return int.Parse(value);
    }

    public static string GetStaffId(this ClaimsPrincipal user)
    {
        return user.FindFirstValue(ClaimTypes.NameIdentifier) ?? "unknown";
    }
}
