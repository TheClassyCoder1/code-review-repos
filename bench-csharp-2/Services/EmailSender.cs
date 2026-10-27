using System.Net.Mail;

namespace Hotel.Services;

public interface IEmailSender
{
    Task SendAsync(string to, string subject, string body, CancellationToken ct = default);
}

public class SmtpEmailSender : IEmailSender
{
    private readonly IConfiguration _config;

    public SmtpEmailSender(IConfiguration config)
    {
        _config = config;
    }

    public async Task SendAsync(string to, string subject, string body, CancellationToken ct = default)
    {
        using var client = new SmtpClient(_config["Smtp:Host"] ?? "localhost");
        using var message = new MailMessage(_config["Smtp:From"] ?? "reservations@hotel.example", to, subject, body);
        await client.SendMailAsync(message, ct);
    }
}
