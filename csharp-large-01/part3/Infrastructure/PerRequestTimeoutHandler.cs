namespace Lending.Servicing.Part3.Infrastructure;

/// <summary>
/// Applies a hard deadline to every request flowing through the pipeline, independent of
/// <see cref="HttpClient.Timeout"/>. Clients that stream large bodies disable the client-level
/// timeout and rely on this handler instead.
/// </summary>
public sealed class PerRequestTimeoutHandler : DelegatingHandler
{
    private readonly TimeSpan _timeout;

    public PerRequestTimeoutHandler(TimeSpan timeout)
    {
        if (timeout <= TimeSpan.Zero || timeout == Timeout.InfiniteTimeSpan)
        {
            throw new ArgumentOutOfRangeException(nameof(timeout), "A finite, positive per-request timeout is required.");
        }

        _timeout = timeout;
    }

    protected override async Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request,
        CancellationToken cancellationToken)
    {
        using var deadline = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        deadline.CancelAfter(_timeout);

        try
        {
            return await base.SendAsync(request, deadline.Token).ConfigureAwait(false);
        }
        catch (OperationCanceledException ex) when (!cancellationToken.IsCancellationRequested)
        {
            throw new TimeoutException(
                $"Request to {request.RequestUri?.Host} exceeded {_timeout.TotalSeconds:0}s.", ex);
        }
    }
}
