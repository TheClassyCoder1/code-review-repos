namespace Lending.Part2.Links;

public sealed record IssuePaymentLinkRequest(string LoanId, decimal Amount, string CheckoutUrl);

public static class PaymentLinkEndpoints
{
    public static IEndpointRouteBuilder MapPaymentLinks(this IEndpointRouteBuilder endpoints)
    {
        endpoints.MapGet("/p/{token}", (string token, IPaymentLinkStore store) =>
        {
            var link = store.Find(token);
            return link is null || link.ExpiresAt <= DateTimeOffset.UtcNow
                ? Results.NotFound()
                : Results.Redirect(link.TargetUrl);
        });

        endpoints.MapPost("/servicing/payment-links", (IssuePaymentLinkRequest request, PaymentLinkIssuer issuer) =>
        {
            try
            {
                var link = issuer.Issue(request.LoanId, request.Amount, request.CheckoutUrl);
                return Results.Ok(new { url = $"/p/{link.Token}", expiresAt = link.ExpiresAt });
            }
            catch (ArgumentException ex)
            {
                return Results.BadRequest(new { error = ex.Message });
            }
        })
        .RequireAuthorization(policy => policy.RequireRole("servicing-ops"));

        return endpoints;
    }
}
