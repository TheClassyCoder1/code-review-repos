import { AppsCasDetails, SignPrincipal } from './casModel';
import { ServiceResponseBuilder } from './serviceResponseBuilder';

export abstract class AbstractAuthorizeAdapter {
  serviceResponseBuilder: ServiceResponseBuilder | null = null;
  protected principal: SignPrincipal | null = null;
  protected app: AppsCasDetails | null = null;

  setPrincipal(principal: SignPrincipal): void {
    this.principal = principal;
  }

  setApp(app: AppsCasDetails): void {
    this.app = app;
  }

  abstract generateInfo(): void;
}

export class CasDefaultAdapter extends AbstractAuthorizeAdapter {
  generateInfo(): void {
    if (!this.serviceResponseBuilder || !this.principal) {
      return;
    }
    this.serviceResponseBuilder.success().setUser(this.principal.username);
    this.serviceResponseBuilder.setAttribute('userId', this.principal.userId);
  }
}

export class CasAttributesAdapter extends CasDefaultAdapter {
  generateInfo(): void {
    super.generateInfo();
    if (!this.serviceResponseBuilder || !this.principal) {
      return;
    }
    for (const [name, value] of Object.entries(this.principal.attributes)) {
      this.serviceResponseBuilder.setAttribute(name, value);
    }
  }
}

const ADAPTERS: Record<string, new () => AbstractAuthorizeAdapter> = {
  CasDefaultAdapter,
  CasAttributesAdapter,
};

/** Creates the adapter class registered for an app. */
export function newAdapterInstance(name: string): AbstractAuthorizeAdapter {
  const ctor = ADAPTERS[name];
  if (!ctor) {
    throw new Error(`unknown CAS adapter ${name}`);
  }
  return new ctor();
}
