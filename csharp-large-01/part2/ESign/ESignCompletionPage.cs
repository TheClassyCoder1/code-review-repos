namespace Lending.Part2.ESign;

/// <summary>
/// The page the signing vendor's iframe lands on once the borrower finishes. It posts
/// the outcome to the portal window that embedded the iframe.
/// </summary>
public sealed class ESignCompletionPage(IConfiguration configuration)
{
    private readonly string _portalOrigin = (configuration["ESign:PortalOrigin"] ?? "https://portal.lending.example").TrimEnd('/');

    public string Render(string envelopeId, string status) => $$"""
        <!DOCTYPE html>
        <html>
        <head><meta charset="utf-8"><title>Signing finished</title></head>
        <body>
        <p id="msg">Envelope {{envelopeId}} is {{status}}. You can close this window.</p>
        <script>
          window.parent.postMessage({ type: "esign.finished", envelopeId: "{{envelopeId}}", status: "{{status}}" }, "{{_portalOrigin}}");
        </script>
        </body>
        </html>
        """;
}
