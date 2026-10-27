namespace Lending.Part2.Bureau;

public sealed class BureauOptions
{
    public const string SectionName = "Bureau";

    public string BaseAddress { get; set; } = string.Empty;

    /// <summary>Targets the bureau sandbox, which is served with a certificate from the bureau's private test CA.</summary>
    public bool UseSandbox { get; set; }
}
