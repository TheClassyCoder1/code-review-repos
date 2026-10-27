using System.Reflection;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part8.Cas;

[ApiController]
public sealed class CasServiceValidateController(
    ITicketServices ticketServices,
    IProxyGrantingTicketServices casProxyGrantingTicketServices,
    HttpRequestAdapter httpRequestAdapter,
    HttpResponseAdapter httpResponseAdapter,
    ILogger<CasServiceValidateController> logger) : ControllerBase
{
    // CAS 3.0 ticket验证接口: 通过ticket获取当前登录用户信息
    [AcceptVerbs("GET", "POST", Route = CasConstants.Endpoint.ServiceValidateV3)]
    public async Task ServiceValidate(
        [FromQuery(Name = CasConstants.Parameter.Ticket)] string ticket,
        [FromQuery(Name = CasConstants.Parameter.Service)] string service,
        [FromQuery(Name = CasConstants.Parameter.ProxyCallbackUrl)] string? pgtUrl,
        [FromQuery(Name = CasConstants.Parameter.Renew)] string? renew,
        [FromQuery(Name = CasConstants.Parameter.Format)] string format = HttpResponseConstants.FormatType.Xml)
    {
        logger.LogDebug("serviceValidate  ticket {Ticket} , service {Service} , pgtUrl {PgtUrl} , renew {Renew} , format {Format}", ticket, service, pgtUrl, renew, format);

        Ticket? storedTicket = null;
        if (ticket.StartsWith(CasConstants.Prefix.ServiceTicketPrefix, StringComparison.Ordinal))
        {
            try
            {
                storedTicket = ticketServices.ConsumeTicket(ticket);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine(e);
            }
        }
        var serviceResponseBuilder = new ServiceResponseBuilder(format);

        if (storedTicket != null)
        {
            var casDetail = storedTicket.CasDetails;
            // serviceValidate接口中service参数必须和ticket中的service一致
            var requestCasService = HttpUtils.RequestUrl(service);
            if (!string.Equals(requestCasService, casDetail.Service, StringComparison.Ordinal))
            {
                logger.LogDebug("Ticket {Ticket} not valid for register service {RegisteredService} , the request service {RequestService}", ticket, casDetail.Service, requestCasService);
                serviceResponseBuilder.Failure()
                    .SetCode(CasConstants.ErrorCode.InvalidService)
                    .SetDescription("Ticket " + ticket + " not valid for this service " + requestCasService);
                await httpResponseAdapter.WriteAsync(Response, serviceResponseBuilder.Build(), format);
            }

            var authentication = storedTicket.Authentication.Principal;
            if (!string.IsNullOrWhiteSpace(pgtUrl))
            {
                var proxyGrantingTicketIOUImpl = new ProxyGrantingTicketIOUImpl();
                var proxyGrantingTicketIOU = casProxyGrantingTicketServices.CreateTicket(proxyGrantingTicketIOUImpl);

                var proxyGrantingTicketImpl = new ProxyGrantingTicketImpl(storedTicket.Authentication, storedTicket.CasDetails);
                var proxyGrantingTicket = casProxyGrantingTicketServices.CreateTicket(proxyGrantingTicketImpl);

                serviceResponseBuilder.Success().SetTicket(proxyGrantingTicketIOU);
                serviceResponseBuilder.Success().SetProxy(pgtUrl);

                await httpRequestAdapter.PostAsync(pgtUrl + "?pgtId=" + proxyGrantingTicket + "&pgtIou=" + proxyGrantingTicketIOU, null);
            }

            if (ConstsBoolean.IsTrue(storedTicket.CasDetails.IsAdapter))
            {
                var casAdapter = Activator.CreateInstance(Type.GetType(storedTicket.CasDetails.Adapter!, throwOnError: true)!)!;
                try
                {
                    casAdapter.GetType().GetProperty("ServiceResponseBuilder")?.SetValue(casAdapter, serviceResponseBuilder);
                }
                catch (Exception e) when (e is MethodAccessException or TargetInvocationException)
                {
                    logger.LogError(e, "setProperty error . ");
                }

                var adapter = (AbstractAuthorizeAdapter)casAdapter;
                adapter.SetPrincipal(authentication);
                adapter.SetApp(storedTicket.CasDetails);
                adapter.GenerateInfo();
            }
            else
            {
                logger.LogError("Cas Adapter is not Set . ");
            }
        }
        else
        {
            serviceResponseBuilder.Failure()
                .SetCode(CasConstants.ErrorCode.InvalidTicket)
                .SetDescription("Ticket " + ticket + " not recognized");
        }

        await httpResponseAdapter.WriteAsync(Response, serviceResponseBuilder.Build(), format);
    }
}
