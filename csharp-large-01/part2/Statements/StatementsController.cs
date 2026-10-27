using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lending.Part2.Statements;

[ApiController]
[Authorize]
[Route("servicing/statements")]
public sealed class StatementsController : ControllerBase
{
    private readonly StatementService _statements;

    public StatementsController(StatementService statements)
    {
        _statements = statements;
    }

    [HttpGet("{loanId}/{year:int}/{month:int}")]
    public async Task<IActionResult> Get(string loanId, int year, int month, CancellationToken ct)
    {
        if (month is < 1 or > 12)
        {
            return BadRequest("month must be between 1 and 12");
        }

        var borrowerId = User.FindFirst(ServicingClaims.BorrowerId)?.Value;
        if (borrowerId is null)
        {
            return Forbid();
        }

        var html = await _statements.RenderHtmlAsync(loanId, borrowerId, year, month, ct);
        return html is null ? NotFound() : Content(html, "text/html; charset=utf-8");
    }
}
