using System.Xml;

namespace LedgerApi.Security;

public static class XmlSecurity
{
    public static XmlReaderSettings ReaderSettings() => new()
    {
        DtdProcessing = DtdProcessing.Prohibit,
        XmlResolver = null,
        MaxCharactersInDocument = 10_000_000,
        IgnoreComments = true,
    };
}
