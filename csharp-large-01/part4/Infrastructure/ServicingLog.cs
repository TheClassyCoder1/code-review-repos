namespace Lending.Servicing.Part4.Infrastructure;

/// <summary>
/// Minimal structured log used by the servicing jobs. Callers pass internal
/// identifiers only (loan ids, request ids, statement ids).
/// </summary>
public interface IServicingLog
{
    void Info(string message, params object[] args);

    void Warn(string message, params object[] args);

    void Error(Exception exception, string message, params object[] args);
}

public sealed class ConsoleServicingLog : IServicingLog
{
    private readonly string _category;

    public ConsoleServicingLog(string category)
    {
        _category = category;
    }

    public void Info(string message, params object[] args) => Write("INFO", message, args);

    public void Warn(string message, params object[] args) => Write("WARN", message, args);

    public void Error(Exception exception, string message, params object[] args)
    {
        Write("ERROR", message, args);
        Console.Error.WriteLine($"  {exception.GetType().Name}: {exception.Message}");
    }

    private void Write(string level, string message, object[] args)
    {
        var text = args.Length == 0 ? message : string.Format(message, args);
        Console.WriteLine($"{DateTimeOffset.UtcNow:O} {level} [{_category}] {text}");
    }
}

public interface IClock
{
    DateTimeOffset UtcNow { get; }
}

public sealed class SystemClock : IClock
{
    public DateTimeOffset UtcNow => DateTimeOffset.UtcNow;
}
