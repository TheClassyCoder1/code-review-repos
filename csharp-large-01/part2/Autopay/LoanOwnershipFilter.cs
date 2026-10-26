using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;

namespace Lending.Part2.Autopay;

/// <summary>
/// Applied to every borrower-facing controller routed under <c>servicing/loans/{loanId}</c>.
/// The loan in the route must belong to the borrower in the session; staff with the
/// <c>servicing-ops</c> role may act on any loan. Mismatches return 404 so loan ids cannot be probed.
/// </summary>
public sealed class LoanOwnershipFilter(ILoanDirectory loans) : IAsyncActionFilter
{
    public const string StaffRole = "servicing-ops";

    public async Task OnActionExecutionAsync(ActionExecutingContext context, ActionExecutionDelegate next)
    {
        var user = context.HttpContext.User;
        if (!user.IsInRole(StaffRole))
        {
            var loanId = context.RouteData.Values["loanId"] as string;
            var borrowerId = user.FindFirst(ServicingClaims.BorrowerId)?.Value;
            var owner = loanId is null ? null : await loans.FindBorrowerIdAsync(loanId, context.HttpContext.RequestAborted);
            if (borrowerId is null || owner is null || !string.Equals(owner, borrowerId, StringComparison.Ordinal))
            {
                context.Result = new NotFoundResult();
                return;
            }
        }

        await next();
    }
}
