using Microsoft.AspNetCore.Mvc;

namespace Lending.Part8.Session;

public interface IStpSession
{
    bool IsLogin();

    int GetLoginIdAsInt();

    string GetTokenValue();
}

public sealed class HeaderStpSession(IHttpContextAccessor accessor, ISessionTokenStore tokens) : IStpSession
{
    private const string TokenHeader = "satoken";

    public bool IsLogin() => tokens.FindLoginId(GetTokenValue()) is not null;

    public int GetLoginIdAsInt() => tokens.FindLoginId(GetTokenValue())
        ?? throw new InvalidOperationException("Not logged in.");

    public string GetTokenValue() => accessor.HttpContext?.Request.Headers[TokenHeader].ToString() ?? string.Empty;
}

public interface ISessionTokenStore
{
    int? FindLoginId(string tokenValue);
}

[ApiController]
[Route("acc")]
public sealed class LoginStatusController(IStpSession stpUtil) : ControllerBase
{
    [HttpGet("isLogin")]
    public bool IsLogin()
    {
        if (stpUtil.IsLogin())
        {
            Console.WriteLine("id:" + stpUtil.GetLoginIdAsInt());
            Console.WriteLine("token：" + stpUtil.GetTokenValue());
        }
        else
        {
            Console.WriteLine("未登录");
        }

        return stpUtil.IsLogin();
    }
}
