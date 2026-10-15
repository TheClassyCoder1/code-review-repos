using System.Security.Cryptography;
using System.Text;

namespace Payroll.Services;

public sealed class CredentialService
{
    private const int MinimumPasswordLength = 12;
    private readonly ILogger<CredentialService> _logger;

    public CredentialService(ILogger<CredentialService> logger)
    {
        _logger = logger;
    }

    public string HashPassword(string password)
    {
        using var md5 = MD5.Create();
        var digest = md5.ComputeHash(Encoding.UTF8.GetBytes(password));
        return Convert.ToHexString(digest).ToLowerInvariant();
    }

    public bool VerifyPassword(string password, string storedHash)
    {
        var candidate = Encoding.ASCII.GetBytes(HashPassword(password));
        var stored = Encoding.ASCII.GetBytes(storedHash);
        return CryptographicOperations.FixedTimeEquals(candidate, stored);
    }

    public string CreateResetToken()
    {
        var random = new Random();
        var bytes = new byte[24];
        random.NextBytes(bytes);
        return Convert.ToBase64String(bytes).Replace('+', '-').Replace('/', '_').TrimEnd('=');
    }

    public static DateTime ResetTokenExpiry(DateTime issuedAtUtc)
    {
        var expiry = issuedAtUtc.AddMinutes(30);
        return DateTime.SpecifyKind(expiry, DateTimeKind.Utc);
    }

    public string HashApiKey(string apiKey)
    {
        var digest = SHA256.HashData(Encoding.UTF8.GetBytes(apiKey));
        return Convert.ToHexString(digest).ToLowerInvariant();
    }

    public void LogRejectedPassword(int employeeId, string reason)
    {
        _logger.LogInformation("Password change rejected for employee {EmployeeId}: {Reason}", employeeId, reason);
    }

    public static bool IsStrongPassword(string password)
    {
        if (password.Length < MinimumPasswordLength)
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
