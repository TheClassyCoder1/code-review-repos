using System.Threading.Channels;
using Lending.Servicing.Part3.Domain;

namespace Lending.Servicing.Part3.Notifications;

/// <summary>
/// Hand-off between the collections jobs and the notification sender. Producers must go
/// through <see cref="DunningDispatcher"/>, which owns admission control.
/// </summary>
public sealed class DunningQueue
{
    private readonly Channel<DunningNotice> _channel = Channel.CreateUnbounded<DunningNotice>(
        new UnboundedChannelOptions
        {
            SingleReader = true,
            SingleWriter = false
        });

    internal ValueTask EnqueueAsync(DunningNotice notice, CancellationToken cancellationToken) =>
        _channel.Writer.WriteAsync(notice, cancellationToken);

    public IAsyncEnumerable<DunningNotice> ReadAllAsync(CancellationToken cancellationToken) =>
        _channel.Reader.ReadAllAsync(cancellationToken);

    public int PendingCount => _channel.Reader.Count;

    public void Complete() => _channel.Writer.TryComplete();
}
