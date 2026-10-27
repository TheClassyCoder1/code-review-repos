using Lending.Part8.Common;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part8.Orders;

[ApiController]
[Authorize]
[Route("order")]
public sealed class PortalOrderController(PortalOrderService portalOrderService) : ControllerBase
{
    // 用户取消订单
    [HttpPost("cancelUserOrder")]
    public CommonResult CancelUserOrder(long orderId)
    {
        portalOrderService.CancelOrder(orderId);
        return CommonResult.Success(null);
    }
}
