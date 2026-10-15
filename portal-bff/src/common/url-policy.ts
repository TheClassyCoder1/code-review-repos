import { BadRequestException, Injectable, PipeTransform } from '@nestjs/common';
import { notifyConfig } from '../config/notify.config';

const DEFAULT_RETURN_PATH = '/portal/documents';

export function assertAllowedCallback(raw: string): URL {
  let url: URL;
  try {
    url = new URL(raw);
  } catch {
    throw new BadRequestException('callbackUrl is not a valid URL');
  }
  if (url.protocol !== 'https:' || !notifyConfig.partnerCallbackHosts.includes(url.hostname)) {
    throw new BadRequestException('callbackUrl host is not allowed');
  }
  return url;
}

export function safeReturnPath(raw: unknown): string {
  if (typeof raw !== 'string' || !raw.startsWith('/') || raw.startsWith('//') || raw.includes('\\')) {
    return DEFAULT_RETURN_PATH;
  }
  return raw;
}

@Injectable()
export class ReturnPathPipe implements PipeTransform<unknown, string> {
  transform(value: unknown): string {
    return safeReturnPath(value);
  }
}
