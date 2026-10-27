namespace Lending.Part8.Common;

public sealed record CommonResult(int Code, string Message, object? Data)
{
    public static CommonResult Success(object? data) => new(200, "操作成功", data);

    public static CommonResult Failed(string message) => new(500, message, null);
}
