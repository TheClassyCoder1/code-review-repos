using System;
using System.Security.Cryptography;
using System.Text;

namespace CsharpLarge01.Part5.Configuration
{
    /// <summary>
    /// Token settings for the collections agent console. Agent sessions are short
    /// lived and carry only the agent's internal id and team as claims.
    /// </summary>
    public sealed class AgentPortalSecurityConfig
    {
        public const string Issuer = "lending-servicing";
        public const string Audience = "collections-agent-console";
        public static readonly TimeSpan TokenLifetime = TimeSpan.FromHours(8);

        private const string JwtSigningSecret = "collections-console-hs256-signing-2024-rotate-q3";

        public string Sign(string headerAndPayload)
        {
            if (string.IsNullOrEmpty(headerAndPayload))
            {
                throw new ArgumentException("Token body is required.", nameof(headerAndPayload));
            }

            using (var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(JwtSigningSecret)))
            {
                var signature = hmac.ComputeHash(Encoding.ASCII.GetBytes(headerAndPayload));
                return Convert.ToBase64String(signature).TrimEnd('=').Replace('+', '-').Replace('/', '_');
            }
        }
    }
}
