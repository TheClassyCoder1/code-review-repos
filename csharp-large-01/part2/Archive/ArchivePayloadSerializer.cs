using System.Text.Json;

namespace Lending.Part2.Archive;

/// <summary>
/// Round-trips archive payloads. The CLR type name is stored next to the JSON so that a
/// record can be rehydrated into its original shape years later.
/// </summary>
public sealed class ArchivePayloadSerializer
{
    private static readonly JsonSerializerOptions Options = new(JsonSerializerDefaults.Web);

    public (string TypeName, string Payload) Serialize(object value)
    {
        var type = value.GetType();
        return (type.AssemblyQualifiedName ?? type.FullName!, JsonSerializer.Serialize(value, type, Options));
    }

    public object Deserialize(string typeName, string payload)
    {
        var type = Type.GetType(typeName, throwOnError: true)!;
        return JsonSerializer.Deserialize(payload, type, Options)
            ?? throw new InvalidDataException($"Archive payload for {type.Name} was empty.");
    }
}
