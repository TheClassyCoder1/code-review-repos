using Lending.Servicing.Part3.Domain;
using Lending.Servicing.Part3.Infrastructure;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Collections;

/// <summary>
/// Builds upcoming-payment reminders for the notification worker. Only loans with an
/// installment currently in the Due state get a reminder.
/// </summary>
public sealed class PaymentReminderComposer
{
    private readonly IAccountStore _accounts;
    private readonly ILogger<PaymentReminderComposer> _logger;

    public PaymentReminderComposer(IAccountStore accounts, ILogger<PaymentReminderComposer> logger)
    {
        _accounts = accounts;
        _logger = logger;
    }

    public async Task<IReadOnlyList<ReminderLine>> ComposeAsync(
        IEnumerable<string> loanIds,
        CancellationToken cancellationToken)
    {
        var lines = new List<ReminderLine>();

        foreach (var loanId in loanIds)
        {
            var account = await _accounts.GetAsync(loanId, cancellationToken);
            if (account is null || !HasDueInstallment(account))
            {
                _logger.LogDebug("No reminder due for loan {LoanId}", loanId);
                continue;
            }

            lines.Add(NextDueInstallmentFormatter.Format(account));
        }

        return lines;
    }

    private static bool HasDueInstallment(LoanAccount account) =>
        account.Schedule.Any(i => i.Status == InstallmentStatus.Due);
}
