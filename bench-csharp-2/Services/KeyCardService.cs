using System.Globalization;

namespace Hotel.Services;

public static class KeyCardService
{
    public static string EncodeKeyCard(string roomNumber, DateOnly checkOut, int sequence)
    {
        var expiry = checkOut.ToDateTime(new TimeOnly(12, 0)).ToString("yyyyMMddHHmm", CultureInfo.InvariantCulture);
        return $"{roomNumber.PadLeft(5, '0')}{expiry}{sequence:D3}";
    }

    public static bool DigestsMatch(string expected, string actual)
    {
        if (expected.Length != actual.Length)
        {
            return false;
        }
        return expected == actual;
    }
}
