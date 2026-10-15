import { Injectable } from '@nestjs/common';
import axios from 'axios';
import { notifyConfig } from '../config/notify.config';

export interface NotifyJob {
  user_id: number;
  channel: 'sms' | 'email' | 'webhook';
  template: string;
  to: string;
  context: Record<string, string>;
}

@Injectable()
export class NotifyWorkerClient {
  async enqueue(job: NotifyJob): Promise<string> {
    const res = await axios.post<{ job_id: string }>(`${notifyConfig.workerUrl}/jobs`, job, {
      headers: { 'x-internal-key': notifyConfig.workerKey },
    });
    return res.data.job_id;
  }

  toJob(userId: number, template: string, context: Record<string, string>, channel: NotifyJob['channel'] = 'email'): NotifyJob {
    return {
      user_id: userId,
      channel,
      template,
      to: `user:${userId}`,
      context: { ...context, sent_at: new Date().toISOString() },
    };
  }
}
