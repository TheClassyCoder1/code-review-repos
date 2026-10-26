using Lending.Part2.Security;

namespace Lending.Part2.Statements;

public sealed class StatementService
{
    private readonly IStatementRepository _repository;
    private readonly StatementTextEncoder _encoder;
    private readonly StatementHtmlRenderer _renderer;
    private readonly ILogger<StatementService> _logger;

    public StatementService(
        IStatementRepository repository,
        StatementTextEncoder encoder,
        StatementHtmlRenderer renderer,
        ILogger<StatementService> logger)
    {
        _repository = repository;
        _encoder = encoder;
        _renderer = renderer;
        _logger = logger;
    }

    public async Task<string?> RenderHtmlAsync(string loanId, string borrowerId, int year, int month, CancellationToken ct)
    {
        var statement = await _repository.FindAsync(loanId, year, month, ct);
        if (statement is null || !string.Equals(statement.BorrowerId, borrowerId, StringComparison.Ordinal))
        {
            return null;
        }

        _logger.LogInformation("Rendering statement for loan {LoanId} period {Year}-{Month}", loanId, year, month);
        return _renderer.Render(_encoder.Encode(statement));
    }

    public async Task<MonthlyStatement> CloseMonthAsync(
        string loanId,
        string borrowerId,
        string borrowerDisplayName,
        int year,
        int month,
        decimal openingBalance,
        IEnumerable<(DateOnly PostedOn, string Description, decimal Amount)> postings,
        CancellationToken ct)
    {
        var running = openingBalance;
        var lines = new List<StatementLine>();
        foreach (var posting in postings.OrderBy(p => p.PostedOn))
        {
            running += posting.Amount;
            lines.Add(new StatementLine(posting.PostedOn, posting.Description, posting.Amount, running));
        }

        var statement = new MonthlyStatement(
            loanId, borrowerId, borrowerDisplayName, year, month, lines, openingBalance, running);
        await _repository.SaveAsync(statement, ct);
        _logger.LogInformation("Closed statement for loan {LoanId} with {LineCount} lines", loanId, lines.Count);
        return statement;
    }
}
