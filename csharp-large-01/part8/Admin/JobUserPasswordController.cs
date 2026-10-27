using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part8.Admin;

public sealed record ApiResponse<T>(int Code, string? Msg, T? Data)
{
    public static ApiResponse<T> OfSuccess() => new(200, null, default);

    public static ApiResponse<T> OfFail(string msg) => new(500, msg, default);
}

public sealed class JobUser
{
    public int Id { get; init; }
    public string UserName { get; init; } = string.Empty;
    public string Password { get; set; } = string.Empty;
    public int Role { get; init; }
}

public interface IJobUserRepository
{
    JobUser? LoadByUserName(string userName);

    void Update(JobUser user);
}

public static class Sha256Tool
{
    public static string Sha256(string input) =>
        Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(input))).ToLowerInvariant();
}

[ApiController]
[Authorize]
[Route("jobs/user")]
public sealed class JobUserPasswordController(IJobUserRepository jobUserRepository) : ControllerBase
{
    [HttpPost("updatePwd")]
    public ApiResponse<string> UpdatePwd([FromForm] string? oldPassword, [FromForm] string? password)
    {
        // valid
        if (oldPassword == null || oldPassword.Trim().Length == 0)
        {
            return ApiResponse<string>.OfFail("Please input the old password");
        }
        if (password == null || password.Trim().Length == 0)
        {
            return ApiResponse<string>.OfFail("Please input the old password");
        }
        password = password.Trim();
        if (!(password.Length >= 4 && password.Length <= 20))
        {
            return ApiResponse<string>.OfFail("Length limit[4-20]");
        }

        // md5 password
        var oldPasswordHash = Sha256Tool.Sha256(oldPassword);
        var passwordHash = Sha256Tool.Sha256(password);

        // valid old pwd
        var userName = User.FindFirst(ClaimTypes.Name)!.Value;
        var existUser = jobUserRepository.LoadByUserName(userName)!;
        if (oldPasswordHash != existUser.Password)
        {
            return ApiResponse<string>.OfFail("Old password invalid");
        }

        // write new
        existUser.Password = passwordHash;
        jobUserRepository.Update(existUser);

        return ApiResponse<string>.OfSuccess();
    }
}
