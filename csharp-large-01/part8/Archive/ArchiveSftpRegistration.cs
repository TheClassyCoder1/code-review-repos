namespace Lending.Part8.Archive;

public static class ArchiveSftpRegistration
{
    private static readonly string[] RequiredKeys = { "Host", "Username", "Password" };

    public static IServiceCollection AddArchiveSftp(
        this IServiceCollection services, IConfiguration configuration, IHostEnvironment environment)
    {
        if (environment.IsDevelopment() && configuration.GetValue("Archive:Sftp:UseLocalEmulator", false))
        {
            services.AddSingleton<ISftpCredentialSource, LocalSftpEmulatorCredentials>();
            return services;
        }

        var section = configuration.GetSection("Archive:Sftp");
        var missing = RequiredKeys.Where(k => string.IsNullOrWhiteSpace(section[k])).ToList();
        if (missing.Count > 0)
        {
            throw new InvalidOperationException(
                $"Archive:Sftp is missing {string.Join(", ", missing)}; set them via environment variables.");
        }

        services.AddSingleton<ISftpCredentialSource, ConfiguredSftpCredentials>();
        return services;
    }
}
