import yaml from 'js-yaml';
import { config } from '../config';
import { HttpError } from '../errors';
import { createJob, JobInput } from './jobService';

export interface FeedItem {
  id?: string | number;
  title?: string;
  description?: string;
  location?: string;
  employment_type?: string;
  remote?: boolean;
}

const EMPLOYMENT_TYPES = new Set(['full_time', 'part_time', 'contract', 'internship']);

export function normalizeFeedItem(item: FeedItem): JobInput {
  const title = (item.title ?? '').trim();
  if (!title) {
    throw new HttpError(422, 'job feed item is missing a title');
  }
  const employmentType = EMPLOYMENT_TYPES.has(item.employment_type ?? '') ? item.employment_type! : 'full_time';
  const location = item.remote ? 'Remote' : (item.location ?? '').trim() || 'Unspecified';
  return {
    title: title.slice(0, 200),
    description: (item.description ?? '').slice(0, 20_000),
    location,
    employmentType,
    externalRef: item.id !== undefined ? String(item.id) : undefined,
  };
}

interface YamlFeed {
  version?: number;
  jobs?: FeedItem[];
}

export async function importJobFromUrl(companyId: string, sourceUrl: string) {
  const response = await fetch(sourceUrl, {
    headers: { accept: 'application/json' },
    signal: AbortSignal.timeout(config.jobFeedTimeoutMs),
  });
  if (!response.ok) {
    throw new HttpError(502, `job source returned ${response.status}`);
  }
  const item = (await response.json()) as FeedItem;
  return createJob(companyId, normalizeFeedItem(item));
}

const MAX_FEED_JOBS = 200;

function assertFeedSize(count: number): void {
  if (count > MAX_FEED_JOBS) {
    throw new HttpError(413, `feed has more than ${MAX_FEED_JOBS} jobs`);
  }
}

export async function importJobsFromYaml(companyId: string, document: string) {
  const feed = yaml.load(document) as YamlFeed | undefined;
  const items = feed?.jobs ?? [];
  assertFeedSize(items.length);
  const created = [];
  for (const item of items) {
    created.push(await createJob(companyId, normalizeFeedItem(item)));
  }
  return { imported: created.length, jobIds: created.map((job) => job.id) };
}
