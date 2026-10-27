using System;
using System.Globalization;
using CsharpLarge01.Part5.Domain;

namespace CsharpLarge01.Part5.Reporting
{
    /// <summary>
    /// Parses one line of the bank's pipe-delimited remittance file:
    /// <c>paymentId|loanId|amount|yyyyMMdd</c>. Malformed lines are rejected rather
    /// than guessed at, so treasury can reconcile them by hand.
    /// </summary>
    public static class RemittanceLineParser
    {
        private const int FieldCount = 4;

        public static bool TryParse(string line, out PaymentReceipt receipt, out string error)
        {
            receipt = null;
            error = null;
            if (string.IsNullOrWhiteSpace(line))
            {
                error = "Empty line.";
                return false;
            }

            var fields = line.Trim().Split('|');
            if (fields.Length != FieldCount)
            {
                error = "Expected " + FieldCount + " fields but found " + fields.Length + ".";
                return false;
            }

            var paymentId = fields[0].Trim();
            var loanId = fields[1].Trim();
            if (paymentId.Length == 0 || loanId.Length == 0)
            {
                error = "Payment id and loan id are required.";
                return false;
            }
            if (!decimal.TryParse(fields[2].Trim(), NumberStyles.AllowDecimalPoint, CultureInfo.InvariantCulture, out var amount)
                || amount <= 0m)
            {
                error = "Amount must be a positive decimal.";
                return false;
            }
            if (!DateTime.TryParseExact(fields[3].Trim(), "yyyyMMdd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var valueDate))
            {
                error = "Value date must be yyyyMMdd.";
                return false;
            }

            receipt = new PaymentReceipt(paymentId, loanId, amount, valueDate);
            return true;
        }
    }
}
