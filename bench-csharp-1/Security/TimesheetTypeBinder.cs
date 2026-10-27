using Newtonsoft.Json.Serialization;
using Payroll.Services;

namespace Payroll.Security;

public sealed class TimesheetTypeBinder : ISerializationBinder
{
    private static readonly Dictionary<string, Type> Allowed = new()
    {
        ["hours"] = typeof(HoursTemplateRow),
        ["allowance"] = typeof(AllowanceTemplateRow),
        ["leave"] = typeof(LeaveTemplateRow)
    };

    public Type BindToType(string? assemblyName, string typeName)
    {
        if (Allowed.TryGetValue(typeName, out var type))
        {
            return type;
        }
        throw new Newtonsoft.Json.JsonSerializationException($"Template row type '{typeName}' is not allowed");
    }

    public void BindToName(Type serializedType, out string? assemblyName, out string? typeName)
    {
        assemblyName = null;
        typeName = Allowed.FirstOrDefault(kv => kv.Value == serializedType).Key;
    }
}
