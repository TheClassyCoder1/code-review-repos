using System.Globalization;
using System.Runtime.CompilerServices;
using Lending.Servicing.Part4.Domain;

namespace Lending.Servicing.Part4.Ledger;

public sealed class LedgerFormatException : FormatException
{
    public LedgerFormatException(int lineNumber, string reason)
        : base($"Ledger line {lineNumber}: {reason}")
    {
        LineNumber = lineNumber;
    }

    public int LineNumber { get; }
}

/// <summary>
/// Reads the core-banking ledger extract (pipe-delimited) in fixed-size batches.
/// Format: entry_id|loan_id|posted_on|kind|amount
/// </summary>
public sealed class LedgerCsvReader
{
    private const char Delimiter = '|';

    private readonly int _batchSize;

    public LedgerCsvReader(int batchSize = 500)
    {
        if (batchSize <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(batchSize), "Batch size must be positive.");
        }

        _batchSize = batchSize;
    }

    public async IAsyncEnumerable<IReadOnlyList<LedgerEntry>> ReadBatchesAsync(
        Stream source,
        [EnumeratorCancellation] CancellationToken cancellationToken)
    {
        var reader = new StreamReader(source, detectEncodingFromByteOrderMarks: true);
        var batch = new List<LedgerEntry>(_batchSize);
        var lineNumber = 0;

        string? line;
        while ((line = await reader.ReadLineAsync(cancellationToken).ConfigureAwait(false)) is not null)
        {
            lineNumber++;
            if (lineNumber == 1 || line.Length == 0 || line[0] == '#')
            {
                continue;
            }

            batch.Add(ParseLine(line, lineNumber));
            if (batch.Count == _batchSize)
            {
                yield return batch;
                batch = new List<LedgerEntry>(_batchSize);
            }
        }

        if (batch.Count > 0)
        {
            yield return batch;
        }
    }

    public static LedgerEntry ParseLine(string line, int lineNumber)
    {
        var fields = line.Split(Delimiter);
        if (fields.Length != 5)
        {
            throw new LedgerFormatException(lineNumber, $"expected 5 fields but found {fields.Length}");
        }

        var entryId = fields[0].Trim();
        var loanId = fields[1].Trim();
        if (entryId.Length == 0 || loanId.Length == 0)
        {
            throw new LedgerFormatException(lineNumber, "entry id and loan id are required");
        }

        if (!DateOnly.TryParseExact(fields[2].Trim(), "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var postedOn))
        {
            throw new LedgerFormatException(lineNumber, "posted_on must be yyyy-MM-dd");
        }

        if (!Enum.TryParse<LedgerEntryKind>(fields[3].Trim(), ignoreCase: true, out var kind) || !Enum.IsDefined(kind))
        {
            throw new LedgerFormatException(lineNumber, "unknown entry kind");
        }

        if (!decimal.TryParse(fields[4].Trim(), NumberStyles.AllowLeadingSign | NumberStyles.AllowDecimalPoint, CultureInfo.InvariantCulture, out var amount))
        {
            throw new LedgerFormatException(lineNumber, "amount is not a decimal number");
        }

        return new LedgerEntry(entryId, loanId, postedOn, kind, amount);
    }
}
