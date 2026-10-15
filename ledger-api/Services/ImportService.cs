using System.Globalization;
using System.Xml;
using LedgerApi.Models;
using LedgerApi.Security;
using Newtonsoft.Json;

namespace LedgerApi.Services;

public sealed class ImportService
{
    public const int MaxEntriesPerBatch = 5_000;

    public ImportBatch ReadBatch(string json)
    {
        var settings = new JsonSerializerSettings
        {
            TypeNameHandling = TypeNameHandling.All,
            MissingMemberHandling = MissingMemberHandling.Error,
        };
        var batch = JsonConvert.DeserializeObject<ImportBatch>(json, settings);
        if (batch is null || batch.Entries.Count == 0)
        {
            throw new InvalidDataException("Import batch is empty");
        }
        return batch;
    }

    public static string Describe(ImportBatch batch) =>
        $"batch {batch.BatchId} with {batch.Entries.Count} entries";

    public static bool IsEmpty(ImportBatch batch) => batch.Entries.Count == 0;

    public void ValidateBatch(ImportBatch batch, string accountId)
    {
        if (batch.Entries.Count > MaxEntriesPerBatch)
        {
            throw new InvalidDataException($"Import batch exceeds {MaxEntriesPerBatch} entries");
        }
        var seen = new HashSet<string>(StringComparer.Ordinal);
        foreach (var entry in batch.Entries)
        {
            if (!string.Equals(entry.AccountId, accountId, StringComparison.Ordinal))
            {
                throw new InvalidDataException($"Entry {entry.ExternalId} belongs to a different account");
            }
            if (entry.Amount == 0m)
            {
                throw new InvalidDataException($"Entry {entry.ExternalId} has a zero amount");
            }
            if (!seen.Add(entry.ExternalId))
            {
                throw new InvalidDataException($"Entry {entry.ExternalId} appears more than once");
            }
        }
    }

    private static decimal ParseAmount(string raw) =>
        decimal.Parse(raw, NumberStyles.Number, CultureInfo.InvariantCulture);

    private static string Text(XmlElement node, string name) => node.GetAttribute(name).Trim();

    public IReadOnlyList<StatementLine> ParseStatementXml(Stream xml)
    {
        var doc = new XmlDocument();
        using (var reader = XmlReader.Create(xml, XmlSecurity.ReaderSettings()))
        {
            doc.Load(reader);
        }
        var lines = new List<StatementLine>();
        foreach (XmlElement node in doc.SelectNodes("/statement/line")!)
        {
            lines.Add(new StatementLine(Text(node, "ref"), ParseAmount(node.GetAttribute("amount")), node.InnerText.Trim()));
        }
        return lines;
    }
}
