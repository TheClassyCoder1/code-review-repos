import { Logger } from '../util/logger';

export const USER_PROFILES = 'pac4jUserProfiles';
export const SOURCE_REQUEST_CONTEXT_URL_ATTRIBUTE_NAME = 'sourceRequestContextUrl';

/** An authenticated profile from the identity provider, including all of its claims. */
export class UserProfile {
  constructor(
    readonly id: string,
    readonly attributes: Record<string, unknown>,
    readonly roles: string[],
    readonly clientName: string,
  ) {}

  getAttribute(name: string): unknown {
    return this.attributes[name];
  }

  toString(): string {
    return `#UserProfile# | id: ${this.id} | attributes: ${JSON.stringify(this.attributes)} | roles: ${this.roles.join(',')} | clientName: ${this.clientName} |`;
  }
}

export interface GatewayRequest {
  attributes: Map<string, unknown>;
}

export interface GatewayResponse {
  statusCode: number;
}

export interface SessionStore {
  get(key: string): unknown;
  set(key: string, value: unknown): void;
}

export interface FilterChain {
  doFilter(request: GatewayRequest, response: GatewayResponse, subject: Subject): Promise<void>;
}

export class PrimaryPrincipal {
  constructor(readonly name: string) {}
}

export class Subject {
  readonly principals = new Set<PrimaryPrincipal>();
}

export interface AuditContext {
  username: string | null;
}

export interface AuditService {
  getContext(): AuditContext;
  attachContext(context: AuditContext): void;
}

export interface Auditor {
  audit(action: string, resource: string | undefined, resourceType: string, outcome: string): void;
}

/** Turns the identity-provider profile on the request into the gateway's primary principal. */
export class ProfileIdentityFilter {
  private readonly logger = new Logger('profile-identity-filter');
  testIdentifier: string | null = null;

  constructor(
    private readonly sessionStore: SessionStore,
    private readonly auditService: AuditService,
    private readonly auditor: Auditor,
    private readonly idAttribute: string | null,
  ) {}

  async doFilter(request: GatewayRequest, response: GatewayResponse, chain: FilterChain): Promise<void> {
    const profiles = this.sessionStore.get(USER_PROFILES) as Map<string, UserProfile> | undefined;
    const profile = profiles ? profiles.values().next().value : undefined;
    if (profile) {
      this.logger.debug('User authenticated as: {}', profile);
      this.sessionStore.set(USER_PROFILES, new Map<string, UserProfile>());
      request.attributes.set(USER_PROFILES, new Map<string, UserProfile>());
      let id: string | null = null;
      if (this.idAttribute !== null) {
        const attribute = profile.getAttribute(this.idAttribute);
        if (attribute !== null && attribute !== undefined) {
          id = String(attribute);
        }
        if (id === null) {
          this.logger.error('Invalid attribute_id: {} configured to be used as principal falling back to default id', this.idAttribute);
        }
      }
      if (id === null) {
        id = profile.id;
      }
      this.testIdentifier = id;
      const pp = new PrimaryPrincipal(id);
      const subject = new Subject();
      subject.principals.add(pp);

      const auditContext = this.auditService.getContext();
      auditContext.username = id;
      this.auditService.attachContext(auditContext);
      const sourceUri = request.attributes.get(SOURCE_REQUEST_CONTEXT_URL_ATTRIBUTE_NAME) as string | undefined;
      this.auditor.audit('authentication', sourceUri, 'uri', 'success');

      await chain.doFilter(request, response, subject);
    }
  }
}
