import { FORMAT_JSON } from './casModel';

export class ServiceResponseBuilder {
  private successful = false;
  private code = '';
  private description = '';
  private ticket = '';
  private proxy = '';
  private user = '';
  private readonly attributes: Record<string, string> = {};

  constructor(private readonly format: string) {}

  success(): this {
    this.successful = true;
    return this;
  }

  failure(): this {
    this.successful = false;
    return this;
  }

  setCode(code: string): this {
    this.code = code;
    return this;
  }

  setDescription(description: string): this {
    this.description = description;
    return this;
  }

  setTicket(ticket: string): this {
    this.ticket = ticket;
    return this;
  }

  setProxy(proxy: string): this {
    this.proxy = proxy;
    return this;
  }

  setUser(user: string): this {
    this.user = user;
    return this;
  }

  setAttribute(name: string, value: string): this {
    this.attributes[name] = value;
    return this;
  }

  serviceResponseBuilder(): string {
    if (this.format.toUpperCase() === FORMAT_JSON) {
      return JSON.stringify(
        this.successful
          ? { serviceResponse: { authenticationSuccess: { user: this.user, proxyGrantingTicket: this.ticket || undefined, attributes: this.attributes } } }
          : { serviceResponse: { authenticationFailure: { code: this.code, description: this.description } } },
      );
    }
    if (!this.successful) {
      return `<cas:serviceResponse xmlns:cas="http://www.yale.edu/tp/cas"><cas:authenticationFailure code="${this.code}">${escapeXml(this.description)}</cas:authenticationFailure></cas:serviceResponse>`;
    }
    const attrs = Object.entries(this.attributes)
      .map(([k, v]) => `<cas:${k}>${escapeXml(v)}</cas:${k}>`)
      .join('');
    const pgt = this.ticket ? `<cas:proxyGrantingTicket>${this.ticket}</cas:proxyGrantingTicket>` : '';
    return `<cas:serviceResponse xmlns:cas="http://www.yale.edu/tp/cas"><cas:authenticationSuccess><cas:user>${escapeXml(this.user)}</cas:user>${pgt}<cas:attributes>${attrs}</cas:attributes></cas:authenticationSuccess></cas:serviceResponse>`;
  }
}

function escapeXml(value: string): string {
  return value.replace(/[<>&"']/g, (c) => `&#${c.charCodeAt(0)};`);
}
