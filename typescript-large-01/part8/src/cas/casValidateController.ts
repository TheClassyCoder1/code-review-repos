import { postRaw } from '../http/httpClient';
import { HttpRequest, HttpResponse } from '../http/types';
import { Logger } from '../util/logger';
import { newAdapterInstance } from './casAdapters';
import {
  CasConstants,
  FORMAT_JSON,
  FORMAT_XML,
  isTrue,
  ProxyGrantingTicketImpl,
  ProxyGrantingTicketIOUImpl,
  ProxyGrantingTicketServices,
  requestUrl,
  SignPrincipal,
  Ticket,
  TicketServices,
} from './casModel';
import { ServiceResponseBuilder } from './serviceResponseBuilder';

export class HttpRequestAdapter {
  async post(url: string, body: string | null): Promise<number> {
    const res = await postRaw(url, body, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } });
    return res.status;
  }
}

export class HttpResponseAdapter {
  write(response: HttpResponse, body: string, format: string): void {
    response.setHeader('Content-Type', format.toUpperCase() === FORMAT_JSON ? 'application/json' : 'application/xml');
    response.send(body);
  }
}

/** CAS 3.0 ticket validation: resolves the logged-in user from a service ticket. */
export class CasValidateController {
  private readonly logger = new Logger('cas-validate');

  constructor(
    private readonly ticketServices: TicketServices,
    private readonly casProxyGrantingTicketServices: ProxyGrantingTicketServices,
    private readonly httpRequestAdapter: HttpRequestAdapter,
    private readonly httpResponseAdapter: HttpResponseAdapter,
  ) {}

  /** GET|POST {@link CasConstants.ENDPOINT.ENDPOINT_SERVICE_VALIDATE_V3} */
  async serviceValidate(request: HttpRequest, response: HttpResponse): Promise<void> {
    const ticket = request.query[CasConstants.PARAMETER.TICKET] ?? '';
    const service = request.query[CasConstants.PARAMETER.SERVICE] ?? '';
    const pgtUrl = request.query[CasConstants.PARAMETER.PROXY_CALLBACK_URL];
    const renew = request.query[CasConstants.PARAMETER.RENEW];
    const format = request.query[CasConstants.PARAMETER.FORMAT] ?? FORMAT_XML;
    this.logger.debug('serviceValidate  ticket {} , service {} , pgtUrl {} , renew {} , format {}', ticket, service, pgtUrl, renew, format);

    let storedTicket: Ticket | null = null;
    if (ticket.startsWith(CasConstants.PREFIX.SERVICE_TICKET_PREFIX)) {
      try {
        storedTicket = await this.ticketServices.consumeTicket(ticket);
      } catch (e) {
        console.error(e);
      }
    }
    const serviceResponseBuilder = new ServiceResponseBuilder(format);

    if (storedTicket != null) {
      const casDetail = storedTicket.casDetails;
      // the service parameter must match the service the ticket was issued for
      const requestCasService = requestUrl(service);
      if (requestCasService !== casDetail.service) {
        this.logger.debug('Ticket {} not valid for register service {} , the request service {}', ticket, casDetail.service, requestCasService);
        serviceResponseBuilder
          .failure()
          .setCode(CasConstants.ERROR_CODE.INVALID_SERVICE)
          .setDescription('Ticket ' + ticket + ' not valid for this service ' + requestCasService);
        this.httpResponseAdapter.write(response, serviceResponseBuilder.serviceResponseBuilder(), format);
      }

      const authentication: SignPrincipal = storedTicket.authentication.principal;
      if (pgtUrl !== undefined && pgtUrl.trim() !== '') {
        const proxyGrantingTicketIOUImpl = new ProxyGrantingTicketIOUImpl();
        const proxyGrantingTicketIOU = await this.casProxyGrantingTicketServices.createTicket(proxyGrantingTicketIOUImpl);

        const proxyGrantingTicketImpl = new ProxyGrantingTicketImpl(storedTicket.authentication, storedTicket.casDetails);
        const proxyGrantingTicket = await this.casProxyGrantingTicketServices.createTicket(proxyGrantingTicketImpl);

        serviceResponseBuilder.success().setTicket(proxyGrantingTicketIOU);
        serviceResponseBuilder.success().setProxy(pgtUrl);

        await this.httpRequestAdapter.post(pgtUrl + '?pgtId=' + proxyGrantingTicket + '&pgtIou=' + proxyGrantingTicketIOU, null);
      }

      if (isTrue(storedTicket.casDetails.isAdapter)) {
        const adapter = newAdapterInstance(storedTicket.casDetails.adapter);
        adapter.serviceResponseBuilder = serviceResponseBuilder;
        adapter.setPrincipal(authentication);
        adapter.setApp(storedTicket.casDetails);
        adapter.generateInfo();
      } else {
        this.logger.error('Cas Adapter is not Set . ');
      }
    } else {
      serviceResponseBuilder
        .failure()
        .setCode(CasConstants.ERROR_CODE.INVALID_TICKET)
        .setDescription('Ticket ' + ticket + ' not recognized');
    }

    this.httpResponseAdapter.write(response, serviceResponseBuilder.serviceResponseBuilder(), format);
  }
}
