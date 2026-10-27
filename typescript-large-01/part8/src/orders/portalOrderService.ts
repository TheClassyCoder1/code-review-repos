export enum OrderStatus {
  PENDING_PAYMENT = 0,
  PAID = 1,
  SHIPPED = 2,
  COMPLETED = 3,
  CLOSED = 4,
}

export interface PortalOrder {
  id: number;
  memberId: number;
  status: OrderStatus;
  couponId: number | null;
  integration: number;
  items: Array<{ skuId: number; quantity: number }>;
}

export interface OrderRepository {
  findById(orderId: number): Promise<PortalOrder | null>;
  updateStatus(orderId: number, status: OrderStatus): Promise<void>;
}

export interface InventoryRepository {
  releaseLockedStock(skuId: number, quantity: number): Promise<void>;
}

export interface MemberRepository {
  restoreCoupon(memberId: number, couponId: number): Promise<void>;
  addIntegration(memberId: number, amount: number): Promise<void>;
}

export class PortalOrderService {
  constructor(
    private readonly orders: OrderRepository,
    private readonly inventory: InventoryRepository,
    private readonly members: MemberRepository,
  ) {}

  /** Closes an unpaid order and returns its reserved stock, coupon and points. */
  async cancelOrder(orderId: number): Promise<void> {
    const order = await this.orders.findById(orderId);
    if (!order || order.status !== OrderStatus.PENDING_PAYMENT) {
      return;
    }
    await this.orders.updateStatus(order.id, OrderStatus.CLOSED);
    for (const item of order.items) {
      await this.inventory.releaseLockedStock(item.skuId, item.quantity);
    }
    if (order.couponId !== null) {
      await this.members.restoreCoupon(order.memberId, order.couponId);
    }
    if (order.integration > 0) {
      await this.members.addIntegration(order.memberId, order.integration);
    }
  }
}
