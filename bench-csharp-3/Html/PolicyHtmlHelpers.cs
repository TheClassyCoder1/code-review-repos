using System.Globalization;
using System.Text.Encodings.Web;
using Microsoft.AspNetCore.Html;
using Microsoft.AspNetCore.Mvc.Rendering;
using PolicyAdmin.Models;

namespace PolicyAdmin.Html;

public static class PolicyHtmlHelpers
{
    private static readonly CultureInfo UsCulture = CultureInfo.GetCultureInfo("en-US");

    public static IHtmlContent EndorsementSummary(this IHtmlHelper html, Endorsement endorsement)
    {
        var builder = new HtmlContentBuilder();
        builder.AppendHtml("<div class=\"endorsement\">");
        builder.AppendHtml($"<span class=\"endorsement-code\">{HtmlEncoder.Default.Encode(endorsement.Code)}</span>");
        builder.AppendHtml($"<p class=\"endorsement-description\">{endorsement.Description}</p>");
        builder.AppendHtml($"<span class=\"endorsement-date\">{endorsement.EffectiveDate:MMM d, yyyy}</span>");
        builder.AppendHtml("</div>");
        return builder;
    }

    private static string CssToken(string value) =>
        new string(value.Where(char.IsLetterOrDigit).ToArray()).ToLowerInvariant();

    public static string StatusCss(string status) => "pill-" + CssToken(status);

    public static IHtmlContent StatusPill(this IHtmlHelper html, string statusLabel)
    {
        var css = StatusCss(statusLabel);
        return new HtmlString($"<span class=\"pill {css}\">{statusLabel}</span>");
    }

    public static string FormatDate(DateOnly date) => date.ToString("MMM d, yyyy", UsCulture);

    public static string FormatTerm(Policy policy) =>
        $"{FormatDate(policy.EffectiveDate)} - {FormatDate(policy.ExpirationDate)}";

    public static IHtmlContent Money(this IHtmlHelper html, decimal amount)
    {
        var text = amount < 0
            ? "(" + Math.Abs(amount).ToString("C2", UsCulture) + ")"
            : amount.ToString("C2", UsCulture);
        var css = amount < 0 ? "money money-negative" : "money";
        var builder = new HtmlContentBuilder();
        builder.AppendHtml($"<span class=\"{css}\">");
        builder.Append(text);
        builder.AppendHtml("</span>");
        return builder;
    }
}
