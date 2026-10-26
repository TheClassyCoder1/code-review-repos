using System.Collections.Concurrent;

namespace Lending.Servicing.Part3.Infrastructure;

/// <summary>
/// Hands out one async mutex per loan id so that read-modify-write cycles on a
/// single account are serialized within the process.
/// </summary>
public sealed class AccountLockProvider
{
    private readonly ConcurrentDictionary<string, SemaphoreSlim> _locks = new(StringComparer.Ordinal);

    public async Task<IAsyncDisposable> AcquireAsync(string loanId, CancellationToken cancellationToken)
    {
        var gate = _locks.GetOrAdd(loanId, _ => new SemaphoreSlim(1, 1));
        await gate.WaitAsync(cancellationToken).ConfigureAwait(false);
        return new Releaser(gate);
    }

    private sealed class Releaser : IAsyncDisposable
    {
        private SemaphoreSlim? _gate;

        public Releaser(SemaphoreSlim gate) => _gate = gate;

        public ValueTask DisposeAsync()
        {
            Interlocked.Exchange(ref _gate, null)?.Release();
            return ValueTask.CompletedTask;
        }
    }
}
