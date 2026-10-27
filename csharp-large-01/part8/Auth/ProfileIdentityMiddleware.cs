using System.Security.Claims;

namespace Lending.Part8.Auth;

public sealed class UserProfile
{
    public string Id { get; init; } = string.Empty;
    public string ClientName { get; init; } = string.Empty;
    public Dictionary<string, object?> Attributes { get; init; } = new();
    public HashSet<string> Roles { get; init; } = new();

    public object? GetAttribute(string name) => Attributes.TryGetValue(name, out var value) ? value : null;

    public override string ToString() =>
        $"#UserProfile# | id: {Id} | clientName: {ClientName} | roles: [{string.Join(", ", Roles)}] | attributes: {{{string.Join(", ", Attributes.Select(a => $"{a.Key}={a.Value}"))}}} |";
}

public interface IProfileManager
{
    UserProfile? GetProfile(HttpContext context);
}

public interface ISessionStore
{
    void Set(HttpContext context, string key, object value);
}

public enum AuditOutcome
{
    Success,
    Failure,
}

public interface IAuditor
{
    void SetUsername(HttpContext context, string username);

    void Audit(string action, string? resource, string resourceType, AuditOutcome outcome);
}

public static class ProfileConstants
{
    public const string UserProfiles = "pac4jUserProfiles";
    public const string SourceRequestUrlItem = "sourceRequestContextUrl";
}

public sealed class ProfileIdentityMiddleware(
    RequestDelegate next,
    IProfileManager manager,
    ISessionStore sessionStore,
    IAuditor auditor,
    IConfiguration configuration,
    ILogger<ProfileIdentityMiddleware> logger)
{
    private readonly string? _idAttribute = configuration["Auth:IdAttribute"];

    public static string? TestIdentifier { get; private set; }

    public async Task InvokeAsync(HttpContext context)
    {
        var profile = manager.GetProfile(context);
        if (profile is not null)
        {
            logger.LogDebug("User authenticated as: {Profile}", profile);
            sessionStore.Set(context, ProfileConstants.UserProfiles, new Dictionary<string, UserProfile>());
            context.Items[ProfileConstants.UserProfiles] = new Dictionary<string, UserProfile>();
            string? id = null;
            if (_idAttribute != null)
            {
                var attribute = profile.GetAttribute(_idAttribute);
                if (attribute != null)
                {
                    id = attribute.ToString();
                }
                if (id == null)
                {
                    logger.LogError("Invalid attribute_id: {IdAttribute} configured to be used as principal falling back to default id", _idAttribute);
                }
            }
            if (id == null)
            {
                id = profile.Id;
            }
            TestIdentifier = id;
            var subject = new ClaimsPrincipal(new ClaimsIdentity(new[] { new Claim(ClaimTypes.NameIdentifier, id) }, "pac4j"));

            auditor.SetUsername(context, id);
            var sourceUri = context.Items[ProfileConstants.SourceRequestUrlItem] as string;
            auditor.Audit("authentication", sourceUri, "uri", AuditOutcome.Success);

            context.User = subject;
            await next(context);
        }
    }
}
