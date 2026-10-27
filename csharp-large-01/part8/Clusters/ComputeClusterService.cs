namespace Lending.Part8.Clusters;

public enum ClusterState
{
    Created,
    Running,
    Cancelling,
    Stopped,
}

public sealed class ComputeCluster
{
    public long Id { get; init; }
    public long OwnerId { get; set; }
    public ClusterState State { get; set; }
    public string? ClusterId { get; set; }
}

public static class ComputeClusterAssembler
{
    public static ComputeCluster? ToEntity(IdRequest? request) =>
        request?.Id is { } id ? new ComputeCluster { Id = id } : null;
}

public interface IComputeClusterRepository
{
    ComputeCluster? FindById(long id);

    void UpdateState(long id, ClusterState state);
}

public interface IClusterDeployer
{
    void Kill(string clusterId);
}

public sealed class ComputeClusterService(
    IComputeClusterRepository clusters,
    IClusterDeployer deployer,
    ILogger<ComputeClusterService> logger)
{
    public bool AllowShutdownCluster(ComputeCluster cluster)
    {
        var stored = clusters.FindById(cluster.Id);
        return stored is { State: ClusterState.Running or ClusterState.Created };
    }

    public void UpdateClusterState(long id, ClusterState state) => clusters.UpdateState(id, state);

    public void Shutdown(ComputeCluster cluster)
    {
        var stored = clusters.FindById(cluster.Id);
        if (stored?.ClusterId is { } clusterId)
        {
            deployer.Kill(clusterId);
        }

        clusters.UpdateState(cluster.Id, ClusterState.Stopped);
        logger.LogInformation("Cluster {ClusterId} shut down", cluster.Id);
    }
}
