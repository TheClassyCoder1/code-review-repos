using System.Globalization;
using System.Net;
using System.Text;
using Payroll.Models;

namespace Payroll.Services;

public sealed class PayslipRenderer
{
    private static readonly CultureInfo UsCulture = CultureInfo.GetCultureInfo("en-US");

    public static string FormatCurrency(decimal amount)
    {
        var rounded = decimal.Round(amount, 2, MidpointRounding.ToEven);
        return rounded < 0
            ? "(" + Math.Abs(rounded).ToString("C", UsCulture) + ")"
            : rounded.ToString("C", UsCulture);
    }

    public string RenderPage(Employee employee, PayRun run, IReadOnlyList<PayslipLine> lines)
    {
        var html = new StringBuilder();
        html.Append("<!doctype html><html><head><meta charset=\"utf-8\"><title>Payslip</title></head><body>");
        html.Append("<h1>").Append(WebUtility.HtmlEncode(employee.FullName)).Append("</h1>");
        html.Append(RenderLinesTable(lines));
        html.Append("</body></html>");
        return html.ToString();
    }

    public static string RenderLinesTable(IReadOnlyList<PayslipLine> lines)
    {
        var table = new StringBuilder("<table class=\"lines\"><tr><th>Item</th><th>Amount</th></tr>");
        foreach (var line in lines)
        {
            table.Append("<tr><td>").Append(line.Description).Append("</td>");
            table.Append("<td class=\"num\">").Append(FormatCurrency(line.Amount)).Append("</td></tr>");
        }
        var total = lines.Sum(l => l.Amount);
        table.Append("<tr class=\"total\"><td>Net pay</td><td class=\"num\">").Append(FormatCurrency(total)).Append("</td></tr>");
        return table.Append("</table>").ToString();
    }

    private static string CssClassFor(string status)
    {
        var normalized = status.Trim().ToLowerInvariant();
        return $"badge badge-{normalized}";
    }

    public static string RenderStatusBadge(string status)
    {
        var css = CssClassFor(status);
        return $"<span class=\"{css}\">{status}</span>";
    }
}
