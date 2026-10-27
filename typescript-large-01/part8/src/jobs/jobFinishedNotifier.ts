import https from 'https';
import { postRaw } from '../http/httpClient';
import { Logger } from '../util/logger';

const log = new Logger('job-finished-notifier');

export interface SnapshotJobInfo {
  database: string;
  table: string;
}

export interface JobInfo {
  jobId: string;
  project: string;
  state: string;
  segmentIds: string[];
  snapshotJobInfo: SnapshotJobInfo | null;
}

export interface NotifierConfig {
  getJobFinishedNotifierUrl(): string | null;
  getJobFinishedNotifierUsername(): string | null;
  getJobFinishedNotifierPassword(): string | null;
}

export function envNotifierConfig(env: NodeJS.ProcessEnv = process.env): NotifierConfig {
  return {
    getJobFinishedNotifierUrl: () => env.JOB_FINISHED_NOTIFIER_URL ?? null,
    getJobFinishedNotifierUsername: () => env.JOB_FINISHED_NOTIFIER_USERNAME ?? null,
    getJobFinishedNotifierPassword: () => env.JOB_FINISHED_NOTIFIER_PASSWORD ?? null,
  };
}

function makeToken(username: string, password: string): string {
  return 'Basic ' + Buffer.from(`${username}:${password}`, 'utf8').toString('base64');
}

const MAX_ATTEMPTS = 3;

/** Posts the final state of each build job to the configured notification endpoint. */
export class JobFinishedNotifier {
  static URL: string | null = null;

  static async postJobInfo(info: JobInfo, config: NotifierConfig = envNotifierConfig()): Promise<void> {
    JobFinishedNotifier.URL = config.getJobFinishedNotifierUrl();
    const URL = JobFinishedNotifier.URL;
    log.info(
      'post job info parameter, url : {}, state : {}, segmentId : {}, table : {}',
      URL,
      info.state,
      info.segmentIds,
      info.snapshotJobInfo != null ? info.snapshotJobInfo.database + '.' + info.snapshotJobInfo.table : null,
    );
    if (URL == null || info.state.toUpperCase() === 'READY') {
      return;
    }

    const agent = new https.Agent({ rejectUnauthorized: false, checkServerIdentity: () => undefined });
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    const username = config.getJobFinishedNotifierUsername();
    const password = config.getJobFinishedNotifierPassword();
    if (username != null && password != null) {
      log.info('use basic auth.');
      headers['Authorization'] = makeToken(username, password);
    }
    try {
      let code = 0;
      for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
        try {
          const response = await postRaw(URL, JSON.stringify(info), { headers, agent, timeoutMs: 3000 });
          code = response.status;
          if (code !== 503 || attempt === MAX_ATTEMPTS) {
            break;
          }
        } catch (e) {
          if (attempt === MAX_ATTEMPTS) {
            throw e;
          }
        }
      }
      if (code === 200) {
        log.info('Post job info to ' + URL + ' successful.');
      } else {
        log.info('Post job info to ' + URL + ' failed. Status code: ' + code);
      }
    } catch (e) {
      log.warn('Error occurred when post job status.', e instanceof Error ? e.message : String(e));
    } finally {
      agent.destroy();
    }
  }
}
