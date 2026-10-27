using System.Collections;
using System.Diagnostics;

namespace Lending.Part8.Storage;

public sealed record RcloneResult(int ExitCode, string Stdout, string Stderr);

public sealed class RcloneExecutionException(RcloneResult result)
    : Exception($"rclone exited with code {result.ExitCode}")
{
    public RcloneResult Result { get; } = result;
}

public sealed class RcloneRunner(
    string rcloneBin,
    IReadOnlyList<string> defaultOptions,
    IReadOnlyDictionary<string, string> envVars,
    ILogger<RcloneRunner> log)
{
    /// <summary>Execute an rclone subcommand.</summary>
    /// <param name="subcommand">Name of the subcommand.</param>
    /// <param name="args">Positional arguments passed to the command.</param>
    /// <param name="options">Options passed to the command.</param>
    public RcloneResult Execute(string subcommand, IReadOnlyList<string> args, IReadOnlyList<string>? options = null)
    {
        options ??= Array.Empty<string>();
        var command = new List<string> { rcloneBin, subcommand };
        command.AddRange(defaultOptions);
        command.AddRange(options);
        command.Add("--");
        command.AddRange(args);

        var env = new Dictionary<string, string>();
        foreach (DictionaryEntry entry in Environment.GetEnvironmentVariables())
        {
            env[(string)entry.Key] = entry.Value?.ToString() ?? string.Empty;
        }
        foreach (var (key, value) in envVars)
        {
            env[key] = value;
        }
        log.LogInformation("Executing rclone command. command={Command}", string.Join(" ", command));
        log.LogDebug("Executing rclone commmad. env={Env}", string.Join(", ", env.Select(kv => $"{kv.Key}={kv.Value}")));

        var startInfo = new ProcessStartInfo(command[0])
        {
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
        };
        foreach (var part in command.Skip(1))
        {
            startInfo.ArgumentList.Add(part);
        }
        startInfo.Environment.Clear();
        foreach (var (key, value) in env)
        {
            startInfo.Environment[key] = value;
        }

        using var process = Process.Start(startInfo)!;
        var stdoutTask = process.StandardOutput.ReadToEndAsync();
        var stderrTask = process.StandardError.ReadToEndAsync();
        process.WaitForExit();
        var result = new RcloneResult(process.ExitCode, stdoutTask.Result, stderrTask.Result);
        if (result.ExitCode != 0)
        {
            throw new RcloneExecutionException(result);
        }

        log.LogDebug(
            "rclone execution finished. stdout={Stdout} stderr={Stderr} exit_code={ExitCode}",
            result.Stdout, result.Stderr, result.ExitCode);
        return result;
    }
}
