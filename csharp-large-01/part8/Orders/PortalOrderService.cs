namespace Lending.Part8.Orders;

public enum PortalOrderStatus
{
    PendingPayment = 0,
    Paid = 1,
    Closed = 4,
}

public sealed class PortalOrder
{
    public long Id { get; init; }
    public long MemberId { get; init; }
    public PortalOrderStatus Status { get; set; }
    public long? CouponId { get; init; }
    public decimal PointsUsed { get; init; }
}

public interface IPortalOrderRepository
{
    PortalOrder? FindById(long orderId);

    void Save(PortalOrder order);
}

public interface IMemberRewardsService
{
    void ReleaseCoupon(long memberId, long couponId);

    void RefundPoints(long memberId, decimal points);
}

public sealed class PortalOrderService(
    IPortalOrderRepository orders,
    IMemberRewardsService rewards,
    ILogger<PortalOrderService> logger)
{
    public void CancelOrder(long orderId)
    {
        var order = orders.FindById(orderId);
        if (order is null || order.Status != PortalOrderStatus.PendingPayment)
        {
            return;
        }

        order.Status = PortalOrderStatus.Closed;
        orders.Save(order);

        if (order.CouponId is { } couponId)
        {
            rewards.ReleaseCoupon(order.MemberId, couponId);
        }

        if (order.PointsUsed > 0)
        {
            rewards.RefundPoints(order.MemberId, order.PointsUsed);
        }

        logger.LogInformation("Order {OrderId} cancelled", order.Id);
    }
}
