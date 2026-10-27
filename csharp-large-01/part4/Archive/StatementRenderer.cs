using System.Globalization;
using System.Text;
using Lending.Servicing.Part4.Domain;

namespace Lending.Servicing.Part4.Archive;

/// <summary>Renders statements into the archive CSV format and summary totals.</summary>
public static class StatementRenderer
{
    public const string Header = "date,description,amount,running_balance";

    public static async Task WriteCsvAsync(Statement statement, TextWriter writer, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(statement);
        ArgumentNullException.ThrowIfNull(writer);

        await writer.WriteLineAsync(Header.AsMemory(), cancellationToken).ConfigureAwait(false);

        var running = statement.OpeningBalance;
        var ordered = statement.Lines
            .Select((line, index) => (line, index))
            .OrderBy(x => x.line.Date)
            .ThenBy(x => x.index)
            .Select(x => x.line);

        foreach (var line in ordered)
        {
            cancellationToken.ThrowIfCancellationRequested();
            running += line.Amount;

            var row = new StringBuilder();
            row.Append(line.Date.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture)).Append(',');
            row.Append(EscapeField(line.Description)).Append(',');
            row.Append(line.Amount.ToString("0.00", CultureInfo.InvariantCulture)).Append(',');
            row.Append(running.ToString("0.00", CultureInfo.InvariantCulture));

            await writer.WriteLineAsync(row.ToString().AsMemory(), cancellationToken).ConfigureAwait(false);
        }
    }

    public static StatementSummary Summarize(Statement statement)
    {
        ArgumentNullException.ThrowIfNull(statement);

        decimal charges = 0m;
        decimal credits = 0m;
        foreach (var line in statement.Lines)
        {
            if (line.Amount >= 0m)
            {
                charges += line.Amount;
            }
            else
            {
                credits += -line.Amount;
            }
        }

        var closing = statement.OpeningBalance + charges - credits;
        return new StatementSummary(
            OpeningBalance: statement.OpeningBalance,
            TotalCharges: decimal.Round(charges, 2, MidpointRounding.ToEven),
            TotalCredits: decimal.Round(credits, 2, MidpointRounding.ToEven),
            ClosingBalance: decimal.Round(closing, 2, MidpointRounding.ToEven),
            LineCount: statement.Lines.Count);
    }

    public static string EscapeField(string value)
    {
        if (string.IsNullOrEmpty(value))
        {
            return string.Empty;
        }

        // Neutralise spreadsheet formula injection before quoting.
        if (value[0] is '=' or '+' or '-' or '@' or '\t' or '\r')
        {
            value = "'" + value;
        }

        var needsQuoting = value.IndexOfAny(new[] { ',', '"', '\n', '\r' }) >= 0;
        if (!needsQuoting)
        {
            return value;
        }

        return "\"" + value.Replace("\"", "\"\"") + "\"";
    }
}
