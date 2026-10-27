using System.Collections.Concurrent;

namespace Lending.Part2.ESign;

public enum ESignStatus
{
    Completed,
    Declined,
    Expired,
    Voided,
}

public sealed record ESignSession(Guid EnvelopeId, string LoanId, string BorrowerId, string ReturnUrl, DateTimeOffset ExpiresAt);

public sealed class ESignSessionStore
{
    private readonly ConcurrentDictionary<Guid, ESignSession> _sessions = new();

    public void Save(ESignSession session) => _sessions[session.EnvelopeId] = session;

    public ESignSession? Take(Guid envelopeId) =>
        _sessions.TryRemove(envelopeId, out var session) && session.ExpiresAt > DateTimeOffset.UtcNow ? session : null;
}
