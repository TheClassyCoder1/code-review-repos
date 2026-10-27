using System.Net.Mail;
using System.Security.Cryptography;
using System.Text;

namespace Lending.Part8.Watches;

public sealed class WatchSettings
{
    public bool ConfirmAnonymousWatches { get; set; } = true;
}

public sealed record WatchUser(long Id, string Email);

public sealed class Watch
{
    public long Id { get; set; }
    public string? ContentType { get; init; }
    public long? ObjectId { get; init; }
    public WatchUser? User { get; init; }
    public string? Email { get; init; }
    public string EventType { get; init; } = string.Empty;
    public string Secret { get; init; } = string.Empty;
    public bool IsActive { get; set; }
}

public interface IWatchStore
{
    Watch? FirstBelongingTo(string eventType, string? contentType, object userOrEmail, long? objectId, IReadOnlyDictionary<string, string> filters);

    Watch Create(Watch watch);

    void CreateFilter(Watch watch, string name, uint value);

    void Delete(Watch watch);
}

public interface IWatchActivationMailer
{
    MailMessage BuildActivationEmail(Watch watch, string email);

    void Send(MailMessage message);
}

public sealed class ActivationRequestFailedException(string recipient, Exception inner)
    : Exception("Activation email could not be delivered.", inner)
{
    public string Recipient { get; } = recipient;
}

public class EventWatchNotifier(IWatchStore watches, IWatchActivationMailer mailer, WatchSettings settings)
{
    protected virtual string EventType => "loan-document-ready";

    protected virtual string? ContentType => "lending.document";

    public Watch Notify(object userOrEmail, long? objectId = null, IReadOnlyDictionary<string, string>? filters = null)
    {
        filters ??= new Dictionary<string, string>();

        // A test-for-existence-then-create race condition exists here, but it
        // doesn't matter: de-duplication on fire and deletion of all matches
        // on stop-notifying nullify its effects.
        var watch = watches.FirstBelongingTo(EventType, ContentType, userOrEmail, objectId, filters);
        if (watch is null)
        {
            var user = userOrEmail as WatchUser;
            var email = userOrEmail as string;
            // Letters that can't be mistaken for other letters or numbers in
            // most fonts, in case people try to type these:
            const string distinguishableLetters = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRTUVWXYZ";
            var random = new Random();
            var secret = new string(Enumerable.Range(0, 10).Select(_ => distinguishableLetters[random.Next(distinguishableLetters.Length)]).ToArray());
            // Registered users don't need to confirm, but anonymous users do.
            var isActive = user is not null || !settings.ConfirmAnonymousWatches;
            watch = watches.Create(new Watch
            {
                ContentType = ContentType,
                ObjectId = objectId is > 0 ? objectId : null,
                User = user,
                Email = email,
                Secret = secret,
                IsActive = isActive,
                EventType = EventType,
            });
            foreach (var (k, v) in filters)
            {
                watches.CreateFilter(watch, k, HashToUnsigned(v));
            }
        }

        // Send email for inactive watches.
        if (!watch.IsActive)
        {
            var email = watch.User is not null ? watch.User.Email : watch.Email!;
            var message = mailer.BuildActivationEmail(watch, email);
            try
            {
                mailer.Send(message);
            }
            catch (SmtpException e)
            {
                watches.Delete(watch);
                throw new ActivationRequestFailedException(email, e);
            }
        }

        return watch;
    }

    private static uint HashToUnsigned(string value)
    {
        var digest = SHA1.HashData(Encoding.UTF8.GetBytes(value));
        return BitConverter.ToUInt32(digest, 0);
    }
}
