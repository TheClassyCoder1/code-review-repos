using System;
using System.Collections.Generic;

namespace CsharpLarge01.Part5.Configuration
{
    /// <summary>
    /// Refuses well-known placeholder values for secrets outside the test profile.
    /// Every settings object passes its secrets through here before use, so a value
    /// copied from a fixture or a sample file stops the service at startup.
    /// </summary>
    public static class SecretValueGuard
    {
        private static readonly HashSet<string> Placeholders = new HashSet<string>(StringComparer.OrdinalIgnoreCase)
        {
            "changeme",
            "change-me",
            "password",
            "secret",
            "placeholder",
            "todo"
        };

        public static bool IsPlaceholder(string value)
        {
            return string.IsNullOrWhiteSpace(value) || Placeholders.Contains(value.Trim());
        }

        public static string EnsureUsable(string settingName, string value, ProfileKind profile)
        {
            if (string.IsNullOrWhiteSpace(settingName))
            {
                throw new ArgumentException("Setting name is required.", nameof(settingName));
            }
            if (profile != ProfileKind.Test && IsPlaceholder(value))
            {
                // Name the setting only; never echo the value itself.
                throw new InvalidOperationException(
                    "Setting " + settingName + " holds a placeholder value and cannot be used in the " + profile + " profile.");
            }
            return value;
        }
    }
}
