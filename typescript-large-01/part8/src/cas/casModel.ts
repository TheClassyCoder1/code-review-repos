export const CasConstants = {
  ENDPOINT: {
    ENDPOINT_SERVICE_VALIDATE_V3: '/authz/cas/p3/serviceValidate',
  },
  PARAMETER: {
    TICKET: 'ticket',
    SERVICE: 'service',
    PROXY_CALLBACK_URL: 'pgtUrl',
    RENEW: 'renew',
    FORMAT: 'format',
  },
  PREFIX: {
    SERVICE_TICKET_PREFIX: 'ST-',
  },
  ERROR_CODE: {
    INVALID_TICKET: 'INVALID_TICKET',
    INVALID_SERVICE: 'INVALID_SERVICE',
  },
} as const;

export const FORMAT_XML = 'XML';
export const FORMAT_JSON = 'JSON';

export interface SignPrincipal {
  userId: string;
  username: string;
  attributes: Record<string, string>;
}

export interface Authentication {
  principal: SignPrincipal;
}

export interface AppsCasDetails {
  appId: string;
  service: string;
  isAdapter: string;
  adapter: string;
}

export interface Ticket {
  id: string;
  authentication: Authentication;
  casDetails: AppsCasDetails;
}

export class ProxyGrantingTicketIOUImpl {
  readonly kind = 'PGTIOU';
}

export class ProxyGrantingTicketImpl {
  readonly kind = 'PGT';
  constructor(readonly authentication: Authentication, readonly casDetails: AppsCasDetails) {}
}

export interface TicketServices {
  consumeTicket(ticketId: string): Promise<Ticket | null>;
}

export interface ProxyGrantingTicketServices {
  createTicket(ticket: ProxyGrantingTicketIOUImpl | ProxyGrantingTicketImpl): Promise<string>;
}

export function isTrue(flag: string | undefined): boolean {
  return flag === '1' || flag?.toLowerCase() === 'true';
}

/** Normalises a service URL the way registered CAS services are stored (no query or fragment). */
export function requestUrl(service: string): string {
  try {
    const url = new URL(service);
    return `${url.protocol}//${url.host}${url.pathname}`;
  } catch {
    return service;
  }
}
