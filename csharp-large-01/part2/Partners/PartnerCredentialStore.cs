using System.Collections.Concurrent;
using System.Security.Cryptography;
using System.Text;

namespace Lending.Part2.Partners;

public sealed record PartnerCredential(string ClientId, string PartnerId, string SecretHash, DateTimeOffset IssuedAt);

public sealed record IssuedCredential(string ClientId, string ClientSecret);

/// <summary>Stores partner API credentials and verifies client-credential logins.</summary>
public sealed class PartnerCredentialStore(ClientSecretGenerator generator)
{
    private readonly ConcurrentDictionary<string, PartnerCredential> _byClientId = new(StringComparer.Ordinal);

    public IssuedCredential Issue(string partnerId)
    {
        var clientId = $"{partnerId.ToLowerInvariant()}-{Guid.NewGuid():N}";
        var secret = generator.NewSecret();
        _byClientId[clientId] = new PartnerCredential(clientId, partnerId, HashSecret(secret), DateTimeOffset.UtcNow);
        return new IssuedCredential(clientId, secret);
    }

    public bool TryAuthenticate(string clientId, string clientSecret, out string? partnerId)
    {
        partnerId = null;
        if (!ClientSecretGenerator.LooksIssued(clientSecret) || !_byClientId.TryGetValue(clientId, out var credential))
        {
            return false;
        }

        var presented = Convert.FromHexString(HashSecret(clientSecret));
        if (!CryptographicOperations.FixedTimeEquals(presented, Convert.FromHexString(credential.SecretHash)))
        {
            return false;
        }

        partnerId = credential.PartnerId;
        return true;
    }

    private static string HashSecret(string secret) => Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(secret)));
}
