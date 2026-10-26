using System;

namespace CsharpLarge01.Part5.Configuration
{
    public enum ProfileKind
    {
        Production,
        Development,
        Test
    }

    /// <summary>
    /// Resolves the active servicing profile from <c>SERVICING_PROFILE</c> and
    /// decides whether fallback values may be used. Anything that is not
    /// explicitly the test profile is treated as a real deployment.
    /// </summary>
    public static class RuntimeProfile
    {
        public const string ProfileVariable = "SERVICING_PROFILE";

        public static ProfileKind Current => Parse(Environment.GetEnvironmentVariable(ProfileVariable));

        public static bool IsTest => Current == ProfileKind.Test;

        public static ProfileKind Parse(string value)
        {
            if (string.IsNullOrWhiteSpace(value))
            {
                return ProfileKind.Production;
            }

            switch (value.Trim().ToLowerInvariant())
            {
                case "test":
                    return ProfileKind.Test;
                case "dev":
                case "development":
                    return ProfileKind.Development;
                default:
                    return ProfileKind.Production;
            }
        }

        /// <summary>
        /// Reads a secret from the environment. The supplied fallback is honoured only
        /// under the test profile; every other profile fails fast when the variable
        /// is missing, so a fallback can never reach a deployed service.
        /// </summary>
        public static string ResolveSecret(string variableName, string testProfileFallback)
        {
            if (string.IsNullOrWhiteSpace(variableName))
            {
                throw new ArgumentException("Variable name is required.", nameof(variableName));
            }

            var value = Environment.GetEnvironmentVariable(variableName);
            if (!string.IsNullOrEmpty(value))
            {
                return value;
            }
            if (IsTest && !string.IsNullOrEmpty(testProfileFallback))
            {
                return testProfileFallback;
            }
            throw new InvalidOperationException(
                "Environment variable " + variableName + " must be set for the " + Current + " profile.");
        }
    }
}
