using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part2.Autopay;

[ApiController]
[Authorize]
[Route("servicing/loans/{loanId}/autopay")]
[ServiceFilter(typeof(LoanOwnershipFilter))]
public sealed class AutopayController(AutopayService autopay) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> Get(string loanId, CancellationToken ct)
    {
        var schedule = await autopay.GetScheduleAsync(loanId, ct);
        if (schedule is null)
        {
            return NotFound();
        }

        return Ok(new
        {
            loanId = schedule.LoanId,
            dayOfMonth = schedule.DayOfMonth,
            amount = schedule.Amount,
            fundingAccount = "****" + schedule.FundingAccountLast4,
            lastCollectedOn = schedule.LastCollectedOn,
        });
    }

    [HttpPost("~/servicing/autopay/runs")]
    [Authorize(Roles = LoanOwnershipFilter.StaffRole)]
    public async Task<IActionResult> Run(CancellationToken ct) => Ok(await autopay.RunDueAsync(DateTimeOffset.UtcNow, ct));
}
