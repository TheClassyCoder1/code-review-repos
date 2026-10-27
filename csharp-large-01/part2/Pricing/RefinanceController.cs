using Lending.Part2.Autopay;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part2.Pricing;

public sealed record RefinanceQuoteRequest(string Product, string Tier, decimal Principal, int TermMonths);

[ApiController]
[Authorize]
[Route("servicing/loans/{loanId}/refinance-quote")]
[ServiceFilter(typeof(LoanOwnershipFilter))]
public sealed class RefinanceController(RateCardService rates) : ControllerBase
{
    [HttpPost]
    public IActionResult Quote(string loanId, [FromBody] RefinanceQuoteRequest request) =>
        Ok(rates.Quote(loanId, request.Product, request.Tier, request.Principal, request.TermMonths));
}
