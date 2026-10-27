using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Text;

namespace CsharpLarge01.Part5.Reporting
{
    /// <summary>
    /// RFC 4180 CSV writer for regulator and partner exports. Cells that a
    /// spreadsheet would evaluate as a formula are prefixed with a quote so an
    /// exported file cannot execute anything when opened.
    /// </summary>
    public sealed class CsvExportWriter
    {
        private static readonly char[] CharactersRequiringQuotes = { ',', '"', '\r', '\n' };
        private static readonly char[] FormulaPrefixes = { '=', '+', '-', '@', '\t', '\r' };

        private readonly TextWriter _output;
        private int? _columnCount;

        public CsvExportWriter(TextWriter output)
        {
            _output = output ?? throw new ArgumentNullException(nameof(output));
        }

        public void WriteRow(IReadOnlyList<object> values)
        {
            if (values == null)
            {
                throw new ArgumentNullException(nameof(values));
            }
            if (_columnCount.HasValue && values.Count != _columnCount.Value)
            {
                throw new InvalidOperationException(
                    "Row has " + values.Count + " columns but the header declared " + _columnCount.Value + ".");
            }
            _columnCount = values.Count;

            var line = new StringBuilder();
            for (var i = 0; i < values.Count; i++)
            {
                if (i > 0)
                {
                    line.Append(',');
                }
                line.Append(EscapeField(FormatValue(values[i])));
            }

            // RFC 4180 mandates CRLF regardless of platform.
            _output.Write(line.ToString());
            _output.Write("\r\n");
        }

        public static string EscapeField(string value)
        {
            if (string.IsNullOrEmpty(value))
            {
                return string.Empty;
            }

            var safe = Array.IndexOf(FormulaPrefixes, value[0]) >= 0 && !IsSignedNumber(value)
                ? "'" + value
                : value;

            if (safe.IndexOfAny(CharactersRequiringQuotes) < 0)
            {
                return safe;
            }
            return "\"" + safe.Replace("\"", "\"\"") + "\"";
        }

        private static bool IsSignedNumber(string value)
        {
            return (value[0] == '-' || value[0] == '+')
                && decimal.TryParse(value, NumberStyles.Number, CultureInfo.InvariantCulture, out _);
        }

        private static string FormatValue(object value)
        {
            switch (value)
            {
                case null:
                    return string.Empty;
                case DateTime date:
                    return date.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
                case decimal amount:
                    return amount.ToString("0.00", CultureInfo.InvariantCulture);
                case IFormattable formattable:
                    return formattable.ToString(null, CultureInfo.InvariantCulture);
                default:
                    return value.ToString();
            }
        }
    }
}
