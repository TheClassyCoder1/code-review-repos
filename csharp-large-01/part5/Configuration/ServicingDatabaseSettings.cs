using System;

namespace CsharpLarge01.Part5.Configuration
{
    /// <summary>
    /// Connection settings for the servicing ledger database (installments,
    /// allocations, promises-to-pay) and its read replica used by the aging report.
    /// </summary>
    public sealed class ServicingDatabaseSettings
    {
        public const string DefaultLedgerUrl =
            "postgresql://servicing_app:Ledg3r-Srv-2024!@db-primary:5432/servicing?sslmode=require";

        public const string DefaultReplicaHost = "db-secondary";

        public ServicingDatabaseSettings(string ledgerUrl, string replicaHost, int commandTimeoutSeconds)
        {
            if (string.IsNullOrWhiteSpace(ledgerUrl))
            {
                throw new ArgumentException("Ledger URL is required.", nameof(ledgerUrl));
            }
            if (commandTimeoutSeconds <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(commandTimeoutSeconds));
            }
            if (Uri.TryCreate(ledgerUrl, UriKind.Absolute, out var parsed))
            {
                var colon = parsed.UserInfo.IndexOf(':');
                if (colon >= 0)
                {
                    SecretValueGuard.EnsureUsable("servicing.ledger.password",
                        Uri.UnescapeDataString(parsed.UserInfo.Substring(colon + 1)), RuntimeProfile.Current);
                }
            }

            LedgerUrl = ledgerUrl;
            ReplicaHost = string.IsNullOrWhiteSpace(replicaHost) ? DefaultReplicaHost : replicaHost;
            CommandTimeoutSeconds = commandTimeoutSeconds;
        }

        public string LedgerUrl { get; }

        public string ReplicaHost { get; }

        public int CommandTimeoutSeconds { get; }

        public static ServicingDatabaseSettings FromEnvironment()
        {
            var url = Environment.GetEnvironmentVariable("SERVICING_LEDGER_URL");
            var replica = Environment.GetEnvironmentVariable("SERVICING_REPLICA_HOST");
            return new ServicingDatabaseSettings(
                string.IsNullOrWhiteSpace(url) ? DefaultLedgerUrl : url,
                replica,
                ReadInt("SERVICING_DB_TIMEOUT_SECONDS", 30));
        }

        private static int ReadInt(string variable, int fallback)
        {
            var raw = Environment.GetEnvironmentVariable(variable);
            return int.TryParse(raw, out var parsed) && parsed > 0 ? parsed : fallback;
        }
    }
}
