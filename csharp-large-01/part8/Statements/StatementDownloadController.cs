using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part8.Statements;

[ApiController]
[Authorize(Roles = "servicing-staff")]
[Route("servicing/loans/{loanId}/statements")]
public sealed class StatementDownloadController(StatementPdfStore store) : ControllerBase
{
    [HttpGet("{period}.pdf")]
    public IActionResult Download(string loanId, string period)
    {
        var stream = store.OpenRead(loanId, period);
        if (stream is null)
        {
            return NotFound();
        }

        // FileStreamResult owns the stream and disposes it once the response body is written.
        return File(stream, "application/pdf", $"statement-{period}.pdf");
    }
}
