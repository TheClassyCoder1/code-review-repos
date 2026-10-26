using Microsoft.Extensions.Options;

namespace Lending.Part2.Bureau;

/// <summary>
/// Startup validation for the bureau connection. The sandbox switch relaxes certificate
/// checks, so it is only honoured in Development; any other environment refuses to boot
/// with it turned on, and the bureau endpoint must always be https.
/// </summary>
public sealed class BureauOptionsValidator(IHostEnvironment environment) : IValidateOptions<BureauOptions>
{
    public ValidateOptionsResult Validate(string? name, BureauOptions options)
    {
        var failures = new List<string>();

        if (!Uri.TryCreate(options.BaseAddress, UriKind.Absolute, out var baseAddress) || baseAddress.Scheme != Uri.UriSchemeHttps)
        {
            failures.Add("Bureau:BaseAddress must be an absolute https URL.");
        }

        if (options.UseSandbox && !environment.IsDevelopment())
        {
            failures.Add($"Bureau:UseSandbox is only permitted in Development (current: {environment.EnvironmentName}).");
        }

        return failures.Count == 0 ? ValidateOptionsResult.Success : ValidateOptionsResult.Fail(failures);
    }
}
