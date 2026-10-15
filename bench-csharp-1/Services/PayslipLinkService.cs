using System.Security.Cryptography;
using System.Text;

namespace Payroll.Services;

public sealed class PayslipLinkService
{
    private const string Alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private readonly byte[] _signingKey;
    private readonly string _publicBaseUrl;

    public PayslipLinkService(IConfiguration configuration)
    {
        _signingKey = Convert.FromBase64String(configuration["Payroll:LinkSigningKey"]
            ?? throw new InvalidOperationException("Payroll:LinkSigningKey is not set"));
        _publicBaseUrl = configuration["Payroll:PublicBaseUrl"] ?? "https://payroll.example.com";
    }

    public string CreateShareCode()
    {
        var random = new Random();
        var code = new char[8];
        for (var i = 0; i < code.Length; i++)
        {
            code[i] = Alphabet[random.Next(Alphabet.Length)];
        }
        return new string(code);
    }

    public string Sign(string code, long expiresAtUnix)
    {
        using var hmac = new HMACSHA256(_signingKey);
        var mac = hmac.ComputeHash(Encoding.UTF8.GetBytes($"{code}|{expiresAtUnix}"));
        return Convert.ToHexString(mac).ToLowerInvariant();
    }

    public bool IsValidSignature(string code, long expiresAtUnix, string signature)
    {
        if (DateTimeOffset.UtcNow.ToUnixTimeSeconds() > expiresAtUnix)
        {
            return false;
        }
        var expected = Encoding.ASCII.GetBytes(Sign(code, expiresAtUnix));
        var actual = Encoding.ASCII.GetBytes(signature.ToLowerInvariant());
        return CryptographicOperations.FixedTimeEquals(expected, actual);
    }

    public string BuildShareUrl(string code, TimeSpan lifetime)
    {
        var expires = DateTimeOffset.UtcNow.Add(lifetime).ToUnixTimeSeconds();
        var signature = Sign(code, expires);
        return $"{_publicBaseUrl}/api/payslips/shared/{code}?exp={expires}&sig={signature}";
    }
}
