using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;

namespace Lending.Part8.Errors;

public sealed record AjaxJson(int Code, string Msg, object? Data)
{
    public static AjaxJson GetNotLogin() => new(401, "未登录，请登录后再次访问", null);

    public static AjaxJson GetNotJur(string msg) => new(403, msg, null);

    public static AjaxJson GetError(string msg) => new(500, msg, null);

    public AjaxJson SetMsg(string msg) => this with { Msg = msg };
}

public sealed class NotLoginException(string message) : Exception(message);

public sealed class NotRoleException(string role) : Exception("无此角色：" + role)
{
    public string Role { get; } = role;
}

public sealed class NotPermissionException(string permission) : Exception("无此权限：" + permission)
{
    public string Permission { get; } = permission;
}

public sealed class DisableServiceException(string service, int level, long disableTime)
    : Exception("此账号已被禁止访问服务：" + service)
{
    public string Service { get; } = service;
    public int Level { get; } = level;
    public long DisableTime { get; } = disableTime;
}

public sealed class GlobalExceptionHandler : IExceptionFilter
{
    public void OnException(ExceptionContext context)
    {
        context.Result = new ObjectResult(HandlerException(context.Exception, context.HttpContext));
        context.ExceptionHandled = true;
    }

    public AjaxJson HandlerException(Exception e, HttpContext http)
    {
        // 打印堆栈，以供调试
        Console.WriteLine("全局异常---------------");
        Console.Error.WriteLine(e);

        // 不同异常返回不同状态码
        AjaxJson aj;
        if (e is NotLoginException ee1)
        {   // 如果是未登录异常
            aj = AjaxJson.GetNotLogin().SetMsg(ee1.Message);
        }
        else if (e is NotRoleException ee2)
        {   // 如果是角色异常
            aj = AjaxJson.GetNotJur("无此角色：" + ee2.Role);
        }
        else if (e is NotPermissionException ee3)
        {   // 如果是权限异常
            aj = AjaxJson.GetNotJur("无此权限：" + ee3.Permission);
        }
        else if (e is DisableServiceException ee4)
        {   // 如果是被封禁异常
            aj = AjaxJson.GetNotJur("当前账号 " + ee4.Service + " 服务已被封禁 (level=" + ee4.Level + ")：" + ee4.DisableTime + "秒后解封");
        }
        else
        {   // 普通异常, 输出：500 + 异常信息
            aj = AjaxJson.GetError(e.Message);
        }

        // 返回给前端
        return aj;
    }
}
