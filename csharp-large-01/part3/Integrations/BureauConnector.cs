using System.Net.Sockets;
using System.Text;
using Lending.Servicing.Part3.Infrastructure;

namespace Lending.Servicing.Part3.Integrations;

/// <summary>
/// One request/response session against the bureau's line protocol. The session owns
/// the socket; disposing it closes the connection.
/// </summary>
public sealed class BureauConnection : IAsyncDisposable
{
    private readonly TcpClient _tcp;
    private readonly StreamReader _reader;
    private readonly StreamWriter _writer;

    internal BureauConnection(TcpClient tcp)
    {
        _tcp = tcp;
        _reader = new StreamReader(tcp.GetStream(), Encoding.ASCII, false, 1024, leaveOpen: true);
        _writer = new StreamWriter(tcp.GetStream(), Encoding.ASCII, 1024, leaveOpen: true) { NewLine = "\r\n", AutoFlush = true };
    }

    public async Task<string?> ExchangeAsync(string line, CancellationToken cancellationToken)
    {
        await _writer.WriteLineAsync(line.AsMemory(), cancellationToken);
        return await _reader.ReadLineAsync(cancellationToken);
    }

    public async ValueTask DisposeAsync()
    {
        await _writer.DisposeAsync();
        _reader.Dispose();
        _tcp.Dispose();
    }
}

/// <summary>Opens sessions to the bureau gateway using the merged servicing settings.</summary>
public sealed class BureauConnector
{
    private readonly string _host;
    private readonly int _port;
    private readonly int _connectTimeoutMs;

    public BureauConnector(IReadOnlyDictionary<string, string> settings)
    {
        _host = settings[SettingsDefaults.BureauHost];
        _port = int.Parse(settings[SettingsDefaults.BureauPort]);
        _connectTimeoutMs = int.Parse(settings[SettingsDefaults.BureauReadTimeoutMs]);
    }

    public async Task<BureauConnection> OpenAsync(CancellationToken cancellationToken)
    {
        var tcp = new TcpClient { NoDelay = true };
        try
        {
            using var connectDeadline = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            connectDeadline.CancelAfter(_connectTimeoutMs);
            await tcp.ConnectAsync(_host, _port, connectDeadline.Token);
            return new BureauConnection(tcp);
        }
        catch
        {
            tcp.Dispose();
            throw;
        }
    }
}
