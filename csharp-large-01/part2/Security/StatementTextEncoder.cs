using System.Text.Encodings.Web;
using Lending.Part2.Statements;

namespace Lending.Part2.Security;

/// <summary>
/// HTML-encodes every free-text field of a statement before it is handed to
/// <see cref="StatementHtmlRenderer"/>. Line descriptions carry payment memos typed by
/// borrowers on the portal and servicer notes, so they must never reach markup raw.
/// </summary>
public sealed class StatementTextEncoder
{
    private readonly HtmlEncoder _encoder;

    public StatementTextEncoder(HtmlEncoder encoder)
    {
        _encoder = encoder;
    }

    public MonthlyStatement Encode(MonthlyStatement statement)
    {
        var lines = statement.Lines
            .Select(line => line with { Description = _encoder.Encode(line.Description) })
            .ToList();

        return statement with
        {
            LoanId = _encoder.Encode(statement.LoanId),
            BorrowerId = _encoder.Encode(statement.BorrowerId),
            BorrowerDisplayName = _encoder.Encode(statement.BorrowerDisplayName),
            Lines = lines,
        };
    }
}
