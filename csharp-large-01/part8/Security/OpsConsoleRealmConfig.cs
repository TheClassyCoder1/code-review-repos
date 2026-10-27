namespace Lending.Part8.Security;

public sealed record RealmAccount(string Username, string Password, IReadOnlyList<string> Roles);

public sealed class TextConfigurationRealm
{
    private readonly Dictionary<string, RealmAccount> _users = new(StringComparer.Ordinal);
    private readonly Dictionary<string, IReadOnlyList<string>> _roles = new(StringComparer.Ordinal);

    public bool CachingEnabled { get; private set; }

    public void SetUserDefinitions(string definitions)
    {
        foreach (var (name, values) in ParseDefinitions(definitions))
        {
            _users[name] = new RealmAccount(name, values[0], values.Skip(1).ToList());
        }
    }

    public void SetRoleDefinitions(string definitions)
    {
        foreach (var (name, values) in ParseDefinitions(definitions))
        {
            _roles[name] = values;
        }
    }

    public void SetCachingEnabled(bool enabled) => CachingEnabled = enabled;

    public bool Authenticate(string username, string password) =>
        _users.TryGetValue(username, out var account) && account.Password == password;

    public IEnumerable<string> PermissionsFor(string username) =>
        _users.TryGetValue(username, out var account)
            ? account.Roles.SelectMany(r => _roles.GetValueOrDefault(r) ?? Array.Empty<string>()).Distinct()
            : Enumerable.Empty<string>();

    private static IEnumerable<(string Name, IReadOnlyList<string> Values)> ParseDefinitions(string definitions) =>
        definitions.Split('\n', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(line => line.Split('=', 2))
            .Select(parts => (parts[0].Trim(), (IReadOnlyList<string>)parts[1].Split(',', StringSplitOptions.TrimEntries)));
}

public static class OpsConsoleRealmConfig
{
    public static IServiceCollection AddOpsConsoleRealm(this IServiceCollection services) =>
        services.AddSingleton(_ => Realm());

    public static TextConfigurationRealm Realm()
    {
        var realm = new TextConfigurationRealm();
        realm.SetUserDefinitions("joe.coder=password,user\n" + "jill.coder=password,admin");

        realm.SetRoleDefinitions("admin=read,write\n" + "user=read");
        realm.SetCachingEnabled(true);
        return realm;
    }
}
