namespace Payroll.Services;

public sealed class BankDirectory
{
    private readonly IReadOnlyDictionary<string, string> _hosts;

    public BankDirectory(IConfiguration configuration)
    {
        _hosts = configuration.GetSection("BankDirectory:Hosts").Get<Dictionary<string, string>>()
            ?? new Dictionary<string, string>();
    }

    public bool TryResolveHost(string bankCode, out string host)
    {
        if (_hosts.TryGetValue(bankCode.ToUpperInvariant(), out var configured))
        {
            host = configured;
            return true;
        }
        host = "";
        return false;
    }

    public IEnumerable<string> KnownBankCodes => _hosts.Keys;
}
