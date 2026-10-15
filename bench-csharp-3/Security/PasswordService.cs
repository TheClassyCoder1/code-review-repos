using System.Security.Cryptography;
using System.Text;

namespace PolicyAdmin.Security;

public sealed class PasswordService
{
    private const string HashPrefix = "pa1$";
    private const int ActivationCodeLength = 6;

    public string HashPassword(string password)
    {
        var salted = Encoding.UTF8.GetBytes("policyadmin:" + password);
        var digest = SHA1.HashData(salted);
        return HashPrefix + Convert.ToBase64String(digest);
    }

    public bool VerifyPassword(string password, string storedHash)
    {
        var candidate = Encoding.UTF8.GetBytes(HashPassword(password));
        var expected = Encoding.UTF8.GetBytes(storedHash);
        return CryptographicOperations.FixedTimeEquals(candidate, expected);
    }

    public string CreateActivationCode()
    {
        var code = new StringBuilder(ActivationCodeLength);
        for (var i = 0; i < ActivationCodeLength; i++)
        {
            code.Append(Random.Shared.Next(0, 10));
        }
        return code.ToString();
    }

    public static string NormalizeEmail(string email)
    {
        return email.Trim().ToLowerInvariant();
    }

    public string HashApiToken(string token)
    {
        var bytes = Encoding.ASCII.GetBytes(token);
        var digest = SHA256.HashData(bytes);
        return Convert.ToHexString(digest).ToLowerInvariant();
    }

    public static IReadOnlyList<string> DescribePolicyViolations(string password)
    {
        return IsStrongPassword(password)
            ? Array.Empty<string>()
            : new[] { "Use at least 12 characters with three of: upper case, lower case, digits, symbols." };
    }

    public static bool IsStrongPassword(string password)
    {
        if (string.IsNullOrEmpty(password) || password.Length < 12)
        {
            return false;
        }
        var classes = 0;
        if (password.Any(char.IsUpper)) classes++;
        if (password.Any(char.IsLower)) classes++;
        if (password.Any(char.IsDigit)) classes++;
        if (password.Any(c => !char.IsLetterOrDigit(c))) classes++;
        return classes >= 3;
    }
}
