using System.Globalization;
using System.Text;

namespace Lending.Part2.Statements;

/// <summary>
/// Produces the printable HTML version of a monthly statement. The PDF pipeline
/// feeds this markup to the headless renderer, and the portal shows it inline.
/// </summary>
public sealed class StatementHtmlRenderer
{
    private static readonly CultureInfo Money = CultureInfo.GetCultureInfo("en-US");

    public string Render(MonthlyStatement statement)
    {
        var sb = new StringBuilder(4096);
        sb.Append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">");
        sb.Append("<title>Loan statement</title>");
        sb.Append("<style>table{border-collapse:collapse}td,th{padding:4px 8px;border-bottom:1px solid #ddd}");
        sb.Append(".num{text-align:right}</style></head><body>");
        sb.Append($"<h1>Statement for {statement.BorrowerDisplayName}</h1>");
        sb.Append($"<p class=\"meta\">Loan {statement.LoanId} &middot; {statement.Year}-{statement.Month:D2}</p>");
        sb.Append($"<p>Opening balance: {statement.OpeningBalance.ToString("C", Money)}</p>");
        sb.Append("<table><thead><tr>");
        sb.Append("<th>Date</th><th>Description</th><th class=\"num\">Amount</th><th class=\"num\">Balance</th>");
        sb.Append("</tr></thead><tbody>");

        foreach (var line in statement.Lines)
        {
            sb.Append("<tr>");
            sb.Append($"<td>{line.PostedOn:yyyy-MM-dd}</td>");
            sb.Append($"<td>{line.Description}</td>");
            sb.Append($"<td class=\"num\">{line.Amount.ToString("C", Money)}</td>");
            sb.Append($"<td class=\"num\">{line.RunningBalance.ToString("C", Money)}</td>");
            sb.Append("</tr>");
        }

        sb.Append("</tbody></table>");
        sb.Append($"<p><strong>Closing balance: {statement.ClosingBalance.ToString("C", Money)}</strong></p>");
        sb.Append("</body></html>");
        return sb.ToString();
    }
}
