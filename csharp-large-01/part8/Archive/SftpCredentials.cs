namespace Lending.Part8.Archive;

public sealed record SftpCredentials(string Host, int Port, string Username, string Password);

public interface ISftpCredentialSource
{
    SftpCredentials Get();
}

/// <summary>Credentials for the throwaway SFTP container in docker-compose.dev.yml.</summary>
public sealed class LocalSftpEmulatorCredentials : ISftpCredentialSource
{
    public SftpCredentials Get() => new("localhost", 2222, "archive", "local-sftp-emulator-2024");
}

public sealed class ConfiguredSftpCredentials(IConfiguration configuration) : ISftpCredentialSource
{
    public SftpCredentials Get()
    {
        var section = configuration.GetSection("Archive:Sftp");
        return new SftpCredentials(
            section["Host"]!,
            section.GetValue("Port", 22),
            section["Username"]!,
            section["Password"]!);
    }
}
