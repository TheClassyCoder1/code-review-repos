using Lending.Part8.Common;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part8.Bpm;

public sealed class BpmOaLeave
{
    public long Id { get; init; }
    public long UserId { get; init; }
    public int Type { get; init; }
    public string Reason { get; init; } = string.Empty;
    public DateTime StartTime { get; init; }
    public DateTime EndTime { get; init; }
    public int Status { get; init; }
    public string? ProcessInstanceId { get; init; }
}

public sealed record BpmOaLeaveRespVo(
    long Id, long UserId, int Type, string Reason, DateTime StartTime, DateTime EndTime, int Status, string? ProcessInstanceId)
{
    public static BpmOaLeaveRespVo? From(BpmOaLeave? leave) => leave is null
        ? null
        : new(leave.Id, leave.UserId, leave.Type, leave.Reason, leave.StartTime, leave.EndTime, leave.Status, leave.ProcessInstanceId);
}

public interface IBpmOaLeaveService
{
    BpmOaLeave? GetLeave(long id);
}

[ApiController]
[Route("bpm/oa/leave")]
public sealed class OaLeaveController(IBpmOaLeaveService leaveService) : ControllerBase
{
    // 获得请假申请
    [HttpGet("get")]
    [Authorize(Policy = "bpm:oa-leave:query")]
    public CommonResult GetLeave([FromQuery(Name = "id")] long id)
    {
        var leave = leaveService.GetLeave(id);
        return CommonResult.Success(BpmOaLeaveRespVo.From(leave));
    }
}
