using System.Security.Cryptography;
using System.Text;
using Hotel.Models;
using Hotel.Services;

namespace Hotel.Security;

public class GuestPinVerifier
{
    private readonly byte[] _blindingKey = RandomNumberGenerator.GetBytes(32);

    public static string DigestPin(int guestId, string pin)
    {
        var salt = Encoding.UTF8.GetBytes($"guest-pin:{guestId}");
        var derived = Rfc2898DeriveBytes.Pbkdf2(pin, salt, 100_000, HashAlgorithmName.SHA256, 32);
        return Convert.ToHexString(derived);
    }

    public bool Verify(Guest guest, string pin)
    {
        if (guest.PinDigest is null || pin.Length is < 4 or > 8)
        {
            return false;
        }
        var expected = Blind(guest.PinDigest);
        var actual = Blind(DigestPin(guest.Id, pin));
        return KeyCardService.DigestsMatch(expected, actual);
    }

    private string Blind(string digest)
    {
        var mac = HMACSHA256.HashData(_blindingKey, Encoding.UTF8.GetBytes(digest));
        return Convert.ToHexString(mac);
    }
}
