using System;
using System.Text;

namespace CsharpLarge01.Part5.Reporting
{
    /// <summary>
    /// Masks bank account numbers and IBANs for statements and agent screens.
    /// Only the trailing digits remain visible; separators are preserved so the
    /// masked value keeps the familiar grouping.
    /// </summary>
    public static class AccountNumberMasker
    {
        public const char MaskCharacter = '*';

        public static string Mask(string accountNumber, int visibleTrailing = 4)
        {
            if (visibleTrailing < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(visibleTrailing));
            }
            if (string.IsNullOrWhiteSpace(accountNumber))
            {
                return string.Empty;
            }

            var significant = 0;
            foreach (var c in accountNumber)
            {
                if (char.IsLetterOrDigit(c))
                {
                    significant++;
                }
            }

            // Short identifiers would be fully revealed by the trailing window; mask
            // at least half of them instead.
            var reveal = Math.Min(visibleTrailing, significant / 2);
            var toMask = significant - reveal;
            var masked = new StringBuilder(accountNumber.Length);
            foreach (var c in accountNumber.Trim())
            {
                if (char.IsLetterOrDigit(c) && toMask > 0)
                {
                    masked.Append(MaskCharacter);
                    toMask--;
                }
                else
                {
                    masked.Append(c);
                }
            }
            return masked.ToString();
        }
    }
}
