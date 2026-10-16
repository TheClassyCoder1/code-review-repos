using System.Globalization;
using System.Net.Sockets;
using System.Text;

namespace GiftCards.Services;

public sealed record ProcessorResult(bool Approved, string ResponseCode, string Reference);

public sealed class ProcessorClient
{
    private static readonly TimeSpan ReversalTimeout = TimeSpan.FromSeconds(8);

    private readonly string _host;
    private readonly int _port;

    public ProcessorClient(IConfiguration configuration)
    {
        _host = configuration["Processor:Host"] ?? throw new InvalidOperationException("Processor:Host missing");
        _port = configuration.GetValue("Processor:Port", 7400);
    }

    public static string BuildAuthMessage(string messageType, string cardCode, decimal amount, string currency, string merchantId)
    {
        var minorUnits = decimal.ToInt64(decimal.Round(amount * 100m, 0, MidpointRounding.AwayFromZero));
        var builder = new StringBuilder();
        builder.Append(messageType);
        builder.Append('|').Append(cardCode);
        builder.Append('|').Append(minorUnits.ToString("D12", CultureInfo.InvariantCulture));
        builder.Append('|').Append(currency.ToUpperInvariant());
        builder.Append('|').Append(merchantId);
        builder.Append('|').Append(DateTime.UtcNow.ToString("yyyyMMddHHmmss", CultureInfo.InvariantCulture));
        return builder.ToString();
    }

    public async Task<ProcessorResult> ReverseAsync(string reference, decimal amount, CancellationToken ct)
    {
        using var cts = CancellationTokenSource.CreateLinkedTokenSource(ct);
        cts.CancelAfter(ReversalTimeout);

        using var client = new TcpClient();
        await client.ConnectAsync(_host, _port, cts.Token);
        await using var stream = client.GetStream();
        var request = Encoding.ASCII.GetBytes(BuildAuthMessage("0400", reference, amount, "USD", "-") + "\n");
        await stream.WriteAsync(request, cts.Token);

        var buffer = new byte[256];
        var read = await stream.ReadAsync(buffer, cts.Token);
        return Parse(Encoding.ASCII.GetString(buffer, 0, read));
    }

    private static ProcessorResult Parse(string response)
    {
        var parts = response.Trim().Split('|');
        var code = parts.Length > 1 ? parts[1] : "96";
        var reference = parts.Length > 2 ? parts[2] : "";
        return new ProcessorResult(code == "00", code, reference);
    }

    public async Task<ProcessorResult> AuthorizeAsync(string cardCode, decimal amount, string currency, string merchantId)
    {
        using var client = new TcpClient();
        await client.ConnectAsync(_host, _port);
        await using var stream = client.GetStream();

        var request = Encoding.ASCII.GetBytes(BuildAuthMessage("0100", cardCode, amount, currency, merchantId) + "\n");
        await stream.WriteAsync(request);

        var buffer = new byte[256];
        var read = await stream.ReadAsync(buffer);
        return Parse(Encoding.ASCII.GetString(buffer, 0, read));
    }
}
