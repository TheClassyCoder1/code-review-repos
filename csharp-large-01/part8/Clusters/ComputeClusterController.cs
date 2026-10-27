using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part8.Clusters;

public sealed class IdRequest
{
    [Required]
    public long? Id { get; set; }
}

public sealed record RestResponseBody<T>(int Code, string Message, T? Data)
{
    public static RestResponseBody<T> Success() => new(0, "success", default);
}

[ApiController]
[Authorize]
[Route("api/compute/cluster")]
public sealed class ComputeClusterController(ComputeClusterService computeClusterService) : ControllerBase
{
    [HttpPost("shutdown")]
    public RestResponseBody<object> Shutdown([FromBody] IdRequest request)
    {
        var cluster = ComputeClusterAssembler.ToEntity(request);
        if (cluster != null && computeClusterService.AllowShutdownCluster(cluster))
        {
            computeClusterService.UpdateClusterState(cluster.Id, ClusterState.Cancelling);
            computeClusterService.Shutdown(cluster);
        }
        return RestResponseBody<object>.Success();
    }
}
