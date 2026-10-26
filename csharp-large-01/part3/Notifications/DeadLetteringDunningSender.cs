using Lending.Servicing.Part3.Domain;

namespace Lending.Servicing.Part3.Notifications;

public interface IDunningDeadLetterStore
{
    Task StoreAsync(DunningNotice notice, string error, CancellationToken cancellationToken);
}

/// <summary>
/// The <see cref="IDunningSender"/> that <see cref="DunningRegistration"/> hands to
/// <see cref="DunningDispatcher"/>. A notice that fails to send is persisted to the dead-letter
/// store before the failure propagates; the collections ops replay job resends from there.
/// </summary>
public sealed class DeadLetteringDunningSender : IDunningSender
{
    private readonly IDunningSender _inner;
    private readonly IDunningDeadLetterStore _deadLetters;

    public DeadLetteringDunningSender(IDunningSender inner, IDunningDeadLetterStore deadLetters)
    {
        _inner = inner;
        _deadLetters = deadLetters;
    }

    public async Task SendAsync(DunningNotice notice, CancellationToken cancellationToken)
    {
        try
        {
            await _inner.SendAsync(notice, cancellationToken);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            // Persist with a fresh token: shutdown must not cost us the record.
            await _deadLetters.StoreAsync(notice, ex.GetType().Name, CancellationToken.None);
            throw;
        }
    }
}
