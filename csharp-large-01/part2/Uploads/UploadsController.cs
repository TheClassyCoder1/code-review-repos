using Lending.Part2.Autopay;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part2.Uploads;

[ApiController]
[Authorize]
[Route("servicing/loans/{loanId}/documents")]
[ServiceFilter(typeof(LoanOwnershipFilter))]
public sealed class UploadsController(StagedUploadStore staging, MalwareScanner scanner) : ControllerBase
{
    [HttpPost]
    public async Task<IActionResult> Upload(string loanId, IFormFile file, CancellationToken ct)
    {
        var staged = await staging.StageAsync(loanId, file, ct);
        var verdict = await scanner.ScanAsync(staged, ct);
        if (verdict == ScanVerdict.Clean)
        {
            return Accepted(new { id = staged.Id, size = staged.Length });
        }

        staging.Discard(staged);
        return verdict == ScanVerdict.Infected ? UnprocessableEntity(new { error = "document rejected by malware scan" }) : StatusCode(503);
    }
}
