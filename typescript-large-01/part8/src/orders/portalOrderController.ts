import { CommonResult } from '../http/types';
import { PortalOrderService } from './portalOrderService';

/** Member-facing order endpoints, mounted under /order. */
export class PortalOrderController {
  constructor(private readonly portalOrderService: PortalOrderService) {}

  /** POST /order/cancelUserOrder — the member cancels one of their orders. */
  async cancelUserOrder(orderId: number): Promise<CommonResult<null>> {
    await this.portalOrderService.cancelOrder(orderId);
    return CommonResult.success(null);
  }
}
