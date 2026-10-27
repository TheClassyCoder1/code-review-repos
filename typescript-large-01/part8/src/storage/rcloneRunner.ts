import { spawnSync, SpawnSyncReturns } from 'child_process';
import { Logger } from '../util/logger';

const log = new Logger('rclone');

export class RcloneExecutionError extends Error {
  constructor(readonly exitCode: number | null, readonly stderr: string) {
    super(`rclone exited with code ${exitCode}`);
  }
}

/**
 * Thin wrapper over the rclone binary. Remote credentials are passed through
 * RCLONE_CONFIG_* environment variables rather than a config file.
 */
export class RcloneRunner {
  constructor(
    private readonly rcloneBin: string,
    private readonly defaultOptions: string[],
    private readonly envVars: Record<string, string>,
  ) {}

  /**
   * Executes an rclone subcommand.
   *
   * @param subcommand name of the subcommand
   * @param args positional arguments passed to the command
   * @param options options passed to the command
   */
  execute(subcommand: string, args: string[], options: string[] = []): SpawnSyncReturns<Buffer> {
    const command = [this.rcloneBin, subcommand, ...this.defaultOptions, ...options, '--', ...args];
    const env: NodeJS.ProcessEnv = { ...process.env };
    Object.assign(env, this.envVars);
    log.info('Executing rclone command.', { command });
    log.debug('Executing rclone commmad.', { env });
    const result = spawnSync(command[0], command.slice(1), { env });
    if (result.error) {
      throw result.error;
    }
    if (result.status !== 0) {
      throw new RcloneExecutionError(result.status, result.stderr.toString());
    }
    log.debug('rclone execution finished.', {
      stdout: result.stdout.toString(),
      stderr: result.stderr.toString(),
      exit_code: result.status,
    });
    return result;
  }
}
