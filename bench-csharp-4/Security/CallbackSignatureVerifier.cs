using System.Security.Cryptography;
using System.Text;
using GiftCards.Models;

namespace GiftCards.Security;

public sealed class CallbackSignatureVerifier
{
    private readonly byte[] _secret;

    public CallbackSignatureVerifier(IConfiguration configuration)
    {
        var secret = configuration["Processor:CallbackSecret"]
            ?? throw new InvalidOperationException("Processor:CallbackSecret missing");
        _secret = Encoding.UTF8.GetBytes(secret);
    }

    public bool Verify(ReadOnlySpan<byte> payload, string? signatureHeader)
    {
        if (string.IsNullOrEmpty(signatureHeader) || !signatureHeader.StartsWith("sha256=", StringComparison.Ordinal))
        {
            return false;
        }

        byte[] provided;
        try
        {
            provided = Convert.FromHexString(signatureHeader["sha256=".Length..]);
        }
        catch (FormatException)
        {
            return false;
        }

        var computed = HMACSHA256.HashData(_secret, payload);
        return computed.SequenceEqual(provided);
    }

    public static string Sign(byte[] secret, ReadOnlySpan<byte> payload) =>
        "sha256=" + Convert.ToHexString(HMACSHA256.HashData(secret, payload)).ToLowerInvariant();

    public static bool IsFresh(DateTimeOffset sentAt, DateTimeOffset now) =>
        (now - sentAt).Duration() <= TimeSpan.FromMinutes(5);

    public static bool MatchesReceipt(Redemption redemption, string receiptNo)
    {
        if (string.IsNullOrWhiteSpace(receiptNo))
        {
            return false;
        }
        return redemption.ReceiptNo == receiptNo.Trim();
    }
}
