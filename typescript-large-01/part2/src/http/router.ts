import { IncomingMessage, ServerResponse } from 'http';
import { errorMessage, log } from '../support/log';
import { HttpError, readJsonBody, sendJson } from './respond';

export interface RequestContext {
  req: IncomingMessage;
  res: ServerResponse;
  params: Record<string, string>;
  query: URLSearchParams;
  userId: string | null;
  body: unknown;
}

export type Handler = (ctx: RequestContext) => Promise<void> | void;
export type Middleware = (ctx: RequestContext, next: () => Promise<void>) => Promise<void> | void;

interface Route {
  method: string;
  segments: string[];
  chain: Array<Middleware | Handler>;
}

export class Router {
  private readonly routes: Route[] = [];

  constructor(private readonly bodyLimitBytes = 12 * 1024 * 1024) {}

  get(path: string, ...chain: [...Middleware[], Handler]): void {
    this.routes.push({ method: 'GET', segments: split(path), chain });
  }

  post(path: string, ...chain: [...Middleware[], Handler]): void {
    this.routes.push({ method: 'POST', segments: split(path), chain });
  }

  async handle(req: IncomingMessage, res: ServerResponse): Promise<void> {
    const url = new URL(req.url ?? '/', 'http://portal.internal');
    const method = (req.method ?? 'GET').toUpperCase();
    const match = this.match(method, url.pathname);
    if (!match) {
      sendJson(res, 404, { error: 'not found' });
      return;
    }
    try {
      const ctx: RequestContext = {
        req,
        res,
        params: match.params,
        query: url.searchParams,
        userId: null,
        body: method === 'POST' ? await readJsonBody(req, this.bodyLimitBytes) : undefined,
      };
      const chain = match.route.chain;
      const dispatch = async (i: number): Promise<void> => {
        if (i < chain.length - 1) {
          await (chain[i] as Middleware)(ctx, () => dispatch(i + 1));
        } else {
          await (chain[i] as Handler)(ctx);
        }
      };
      await dispatch(0);
    } catch (err) {
      if (err instanceof HttpError) {
        if (!res.headersSent) sendJson(res, err.status, { error: err.message });
        return;
      }
      log.error('unhandled request failure', { method, path: url.pathname, error: errorMessage(err) });
      if (!res.headersSent) sendJson(res, 500, { error: 'internal error' });
    }
  }

  private match(method: string, pathname: string): { route: Route; params: Record<string, string> } | null {
    const parts = split(pathname);
    for (const route of this.routes) {
      if (route.method !== method || route.segments.length !== parts.length) continue;
      const params: Record<string, string> = {};
      const ok = route.segments.every((seg, i) => {
        if (seg.startsWith(':')) {
          params[seg.slice(1)] = decodeURIComponent(parts[i]);
          return true;
        }
        return seg === parts[i];
      });
      if (ok) return { route, params };
    }
    return null;
  }
}

function split(path: string): string[] {
  return path.split('/').filter((p) => p.length > 0);
}
