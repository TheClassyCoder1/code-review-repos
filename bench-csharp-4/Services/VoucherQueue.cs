using System.Threading.Channels;
using GiftCards.Models;

namespace GiftCards.Services;

public sealed class VoucherQueue
{
    private readonly Channel<VoucherIssueRequest> _channel;

    public VoucherQueue(Channel<VoucherIssueRequest> channel) => _channel = channel;

    public Task EnqueueAsync(VoucherIssueRequest request, CancellationToken ct)
    {
        _ = _channel.Writer.WriteAsync(request, ct);
        return Task.CompletedTask;
    }

    public IAsyncEnumerable<VoucherIssueRequest> ReadAllAsync(CancellationToken ct) =>
        _channel.Reader.ReadAllAsync(ct);
}
