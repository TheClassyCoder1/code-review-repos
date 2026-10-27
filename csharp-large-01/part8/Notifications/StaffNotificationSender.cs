using System.Globalization;
using System.Net.Mail;

namespace Lending.Part8.Notifications;

public sealed record StaffUser(long Id, string? Email, bool IsActive, string PreferredLanguage);

public interface IStaffProfileStore
{
    bool WantsNotification(StaffUser user, string notification);
}

public interface ITemplateRenderer
{
    string Render(string templateName, IReadOnlyDictionary<string, object?> context);
}

public sealed class NotificationMailOptions
{
    public string FromAddress { get; set; } = "servicing-noreply@lending.example";
    public string SmtpHost { get; set; } = "localhost";
    public int SmtpPort { get; set; } = 25;
    public bool UseHtml { get; set; }
}

public sealed class StaffNotificationSender(
    IStaffProfileStore profiles,
    ITemplateRenderer templates,
    NotificationMailOptions options,
    ILogger<StaffNotificationSender> logger)
{
    public bool SendNotification(IEnumerable<StaffUser> recipientUsers, string notification, IDictionary<string, object?> extraContext)
    {
        // Get list of email addresses
        var emailRecipients = recipientUsers
            .Where(r => r.IsActive && !string.IsNullOrEmpty(r.Email) && profiles.WantsNotification(r, notification))
            .ToList();

        // Return if there are no email addresses
        if (emailRecipients.Count == 0)
        {
            return true;
        }

        // Get template
        var templateSubject = "servicing/notifications/" + notification + "_subject.txt";
        var templateText = "servicing/notifications/" + notification + ".txt";
        var templateHtml = "servicing/notifications/" + notification + ".html";

        // Common context to template
        var context = new Dictionary<string, object?> { ["settings"] = options };
        foreach (var (key, value) in extraContext)
        {
            context[key] = value;
        }

        using var connection = new SmtpClient(options.SmtpHost, options.SmtpPort);

        // Send emails
        var sentCount = 0;
        foreach (var recipient in emailRecipients)
        {
            // update context with this recipient
            context["user"] = recipient;

            // Translate text to the recipient language settings
            string emailSubject;
            string emailContent;
            var previousCulture = CultureInfo.CurrentUICulture;
            CultureInfo.CurrentUICulture = CultureInfo.GetCultureInfo(recipient.PreferredLanguage);
            try
            {
                // Get email subject and content
                emailSubject = templates.Render(templateSubject, context).Trim();
                emailContent = templates.Render(templateText, context).Trim();
            }
            finally
            {
                CultureInfo.CurrentUICulture = previousCulture;
            }

            using var message = new MailMessage(options.FromAddress, recipient.Email!, emailSubject, emailContent);
            if (options.UseHtml)
            {
                message.AlternateViews.Add(AlternateView.CreateAlternateViewFromString(
                    templates.Render(templateHtml, context), null, "text/html"));
            }

            try
            {
                // Send email
                connection.Send(message);
                sentCount++;
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Failed to send notification email '{Subject}' to {Email}", emailSubject, recipient.Email);
            }
        }

        return sentCount == emailRecipients.Count;
    }
}
