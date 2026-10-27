using System.Security.Cryptography;
using System.Text;

namespace Hotel.Security;

public class WebhookSignatureVerifier
{
    private readonly byte[] _secret;

    public WebhookSignatureVerifier(IConfiguration config)
    {
        var secret = config["ChannelManager:WebhookSecret"]
            ?? throw new InvalidOperationException("ChannelManager:WebhookSecret is not configured.");
        _secret = Encoding.UTF8.GetBytes(secret);
    }

    public string ComputeSignature(long timestamp, byte[] body)
    {
        var prefix = Encoding.UTF8.GetBytes($"{timestamp}.");
        var material = new byte[prefix.Length + body.Length];
        prefix.CopyTo(material, 0);
        body.CopyTo(material, prefix.Length);
        return Convert.ToHexString(HMACSHA256.HashData(_secret, material)).ToLowerInvariant();
    }

    public static bool IsFresh(long timestamp, DateTimeOffset now)
    {
        var age = now.ToUnixTimeSeconds() - timestamp;
        return age is >= -30 and <= 300;
    }

    public bool IsValid(string? signatureHeader, long timestamp, byte[] body)
    {
        if (string.IsNullOrEmpty(signatureHeader) || !IsFresh(timestamp, DateTimeOffset.UtcNow))
        {
            return false;
        }
        var expected = ComputeSignature(timestamp, body);
        return expected == signatureHeader.Trim().ToLowerInvariant();
    }
}
