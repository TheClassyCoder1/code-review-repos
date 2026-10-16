using System.Security.Claims;

namespace GiftCards.Controllers;

public static class ClaimsExtensions
{
    public static string GetOwnerId(this ClaimsPrincipal user) =>
        user.FindFirstValue(ClaimTypes.NameIdentifier) ?? throw new InvalidOperationException("Missing subject claim");

    public static string? GetMerchantId(this ClaimsPrincipal user) =>
        user.FindFirstValue("merchant_id");
}
