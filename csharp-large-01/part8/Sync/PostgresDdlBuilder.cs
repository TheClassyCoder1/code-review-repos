namespace Lending.Part8.Sync;

public sealed record TablePath(string Schema, string Table);

public sealed record Column(string Name, string DataType, bool Nullable, string? Comment);

public sealed class PostgresDdlBuilder
{
    public IReadOnlyList<string> BuildCreateTableSql(TablePath tablePath, IReadOnlyList<Column> columns)
    {
        var definitions = columns.Select(c =>
            $"{QuoteIdentifier(c.Name)} {c.DataType}{(c.Nullable ? string.Empty : " NOT NULL")}");
        var statements = new List<string>
        {
            $"CREATE TABLE {TableIdentifier(tablePath)} ({string.Join(", ", definitions)})",
        };
        statements.AddRange(columns.Where(c => !string.IsNullOrEmpty(c.Comment)).Select(c => BuildColumnCommentSql(tablePath, c)));
        return statements;
    }

    private string BuildColumnCommentSql(TablePath tablePath, Column column)
    {
        return string.Format(
                "COMMENT ON COLUMN {0}.{1} IS '{2}'",
                TableIdentifier(tablePath), QuoteIdentifier(column.Name), column.Comment);
    }

    private static string TableIdentifier(TablePath tablePath) =>
        QuoteIdentifier(tablePath.Schema) + "." + QuoteIdentifier(tablePath.Table);

    private static string QuoteIdentifier(string identifier) =>
        "\"" + identifier.Replace("\"", "\"\"") + "\"";
}
