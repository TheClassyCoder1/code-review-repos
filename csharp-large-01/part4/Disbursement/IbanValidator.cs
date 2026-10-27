namespace Lending.Servicing.Part4.Disbursement;

/// <summary>ISO 13616 IBAN structure and checksum validation.</summary>
public static class IbanValidator
{
    private const int MinLength = 15;
    private const int MaxLength = 34;

    public static bool IsValid(string? iban)
    {
        if (string.IsNullOrWhiteSpace(iban))
        {
            return false;
        }

        var compact = iban.Replace(" ", string.Empty).ToUpperInvariant();
        if (compact.Length < MinLength || compact.Length > MaxLength)
        {
            return false;
        }

        if (!char.IsAsciiLetterUpper(compact[0]) || !char.IsAsciiLetterUpper(compact[1])
            || !char.IsAsciiDigit(compact[2]) || !char.IsAsciiDigit(compact[3]))
        {
            return false;
        }

        // Country code and check digits move to the end; mod 97 is computed incrementally.
        var remainder = 0;
        foreach (var ch in compact[4..] + compact[..4])
        {
            if (char.IsAsciiDigit(ch))
            {
                remainder = (remainder * 10 + (ch - '0')) % 97;
            }
            else if (char.IsAsciiLetterUpper(ch))
            {
                remainder = (remainder * 100 + (ch - 'A' + 10)) % 97;
            }
            else
            {
                return false;
            }
        }

        return remainder == 1;
    }

    /// <summary>Returns only the country code and last four characters, for logs and receipts.</summary>
    public static string Redact(string iban)
    {
        var compact = iban.Replace(" ", string.Empty);
        return compact.Length <= 6 ? new string('*', compact.Length) : compact[..2] + "****" + compact[^4..];
    }
}
