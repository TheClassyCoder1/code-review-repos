import { HttpError } from '../http/types';

export enum ClusterState {
  CREATED = 'CREATED',
  RUNNING = 'RUNNING',
  CANCELLING = 'CANCELLING',
  CANCELED = 'CANCELED',
  FAILED = 'FAILED',
}

export interface FlinkCluster {
  id: number;
  clusterName?: string;
  userId?: number;
  clusterState?: ClusterState;
}

export interface IdRequest {
  id: number;
}

export class RestResponseBody<T> {
  private constructor(readonly code: number, readonly message: string, readonly data: T | null) {}

  static success<T>(data: T | null = null): RestResponseBody<T> {
    return new RestResponseBody<T>(0, 'success', data);
  }
}

export const FlinkClusterAssembler = {
  toEntity(request: IdRequest | null | undefined): FlinkCluster | null {
    if (!request || !Number.isInteger(request.id) || request.id <= 0) {
      return null;
    }
    return { id: request.id };
  },
};

export interface FlinkClusterRepository {
  findById(id: number): Promise<FlinkCluster | null>;
  updateState(id: number, state: ClusterState): Promise<void>;
  countRunningJobs(clusterId: number): Promise<number>;
}

export interface FlinkClusterClient {
  shutdownSession(cluster: FlinkCluster): Promise<void>;
}

export class FlinkClusterService {
  constructor(private readonly clusters: FlinkClusterRepository, private readonly client: FlinkClusterClient) {}

  /** A session cluster can be stopped once it is running and has no jobs on it. */
  async allowShutdownCluster(cluster: FlinkCluster): Promise<boolean> {
    const stored = await this.clusters.findById(cluster.id);
    if (!stored || stored.clusterState !== ClusterState.RUNNING) {
      return false;
    }
    return (await this.clusters.countRunningJobs(cluster.id)) === 0;
  }

  async updateClusterState(id: number, state: ClusterState): Promise<void> {
    await this.clusters.updateState(id, state);
  }

  async shutdown(cluster: FlinkCluster): Promise<void> {
    const stored = await this.clusters.findById(cluster.id);
    if (!stored) {
      throw new HttpError(404, 'cluster not found');
    }
    try {
      await this.client.shutdownSession(stored);
      await this.clusters.updateState(stored.id, ClusterState.CANCELED);
    } catch (e) {
      await this.clusters.updateState(stored.id, ClusterState.FAILED);
      throw e;
    }
  }
}

/** Cluster management endpoints, mounted under /flink/cluster. */
export class FlinkClusterController {
  constructor(private readonly flinkClusterService: FlinkClusterService) {}

  /** POST /flink/cluster/shutdown */
  async shutdown(request: IdRequest): Promise<RestResponseBody<void>> {
    const cluster = FlinkClusterAssembler.toEntity(request);
    if (cluster != null && (await this.flinkClusterService.allowShutdownCluster(cluster))) {
      await this.flinkClusterService.updateClusterState(cluster.id, ClusterState.CANCELLING);
      await this.flinkClusterService.shutdown(cluster);
    }
    return RestResponseBody.success();
  }
}
