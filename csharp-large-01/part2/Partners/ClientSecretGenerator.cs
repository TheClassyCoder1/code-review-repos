using System.Security.Cryptography;

namespace Lending.Part2.Partners;

/// <summary>
/// Issues partner API client secrets. Secrets are always minted here from 32 bytes of
/// CSPRNG output (256 bits of entropy) and shown to the partner exactly once; partners
/// cannot choose or upload their own secret, so a stored secret is never a
/// human-chosen password.
/// </summary>
public sealed class ClientSecretGenerator
{
    private const int SecretBytes = 32;
    private const string Prefix = "svcp_";

    public string NewSecret()
    {
        var bytes = RandomNumberGenerator.GetBytes(SecretBytes);
        return Prefix + Convert.ToBase64String(bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_');
    }

    public static bool LooksIssued(string candidate)
    {
        return candidate.StartsWith(Prefix, StringComparison.Ordinal) && candidate.Length == Prefix.Length + 43;
    }
}
