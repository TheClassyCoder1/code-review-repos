using System;
using System.Security.Cryptography;
using System.Text;

namespace CsharpLarge01.Part5.Configuration
{
    /// <summary>
    /// Verifies HMAC signatures on payment-processor webhooks before the payment is
    /// handed to the allocator. The shared key comes from the environment.
    /// </summary>
    public sealed class WebhookSigningKeyProvider
    {
        public const string KeyVariable = "SERVICING_WEBHOOK_SIGNING_KEY";

        private const string TestProfileFallbackKey = "local-test-webhook-key";

        private readonly byte[] _key;

        public WebhookSigningKeyProvider()
            : this(RuntimeProfile.ResolveSecret(KeyVariable, TestProfileFallbackKey))
        {
        }

        public WebhookSigningKeyProvider(string key)
        {
            if (string.IsNullOrEmpty(key))
            {
                throw new ArgumentException("Signing key is required.", nameof(key));
            }
            _key = Encoding.UTF8.GetBytes(SecretValueGuard.EnsureUsable(KeyVariable, key, RuntimeProfile.Current));
        }

        public bool Verify(string body, string signatureHex)
        {
            if (body == null || string.IsNullOrWhiteSpace(signatureHex))
            {
                return false;
            }

            byte[] expected;
            using (var hmac = new HMACSHA256(_key))
            {
                expected = hmac.ComputeHash(Encoding.UTF8.GetBytes(body));
            }

            byte[] provided;
            try
            {
                provided = Convert.FromHexString(signatureHex.Trim());
            }
            catch (FormatException)
            {
                return false;
            }
            return CryptographicOperations.FixedTimeEquals(expected, provided);
        }
    }
}
