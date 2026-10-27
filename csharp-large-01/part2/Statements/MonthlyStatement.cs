namespace Lending.Part2.Statements;

public sealed record StatementLine(DateOnly PostedOn, string Description, decimal Amount, decimal RunningBalance);

public sealed record MonthlyStatement(
    string LoanId,
    string BorrowerId,
    string BorrowerDisplayName,
    int Year,
    int Month,
    IReadOnlyList<StatementLine> Lines,
    decimal OpeningBalance,
    decimal ClosingBalance);

public interface IStatementRepository
{
    Task<MonthlyStatement?> FindAsync(string loanId, int year, int month, CancellationToken ct);

    Task SaveAsync(MonthlyStatement statement, CancellationToken ct);
}

public sealed class InMemoryStatementRepository : IStatementRepository
{
    private readonly Dictionary<(string LoanId, int Year, int Month), MonthlyStatement> _statements = new();
    private readonly object _gate = new();

    public Task<MonthlyStatement?> FindAsync(string loanId, int year, int month, CancellationToken ct)
    {
        lock (_gate)
        {
            _statements.TryGetValue((loanId, year, month), out var statement);
            return Task.FromResult(statement);
        }
    }

    public Task SaveAsync(MonthlyStatement statement, CancellationToken ct)
    {
        lock (_gate)
        {
            _statements[(statement.LoanId, statement.Year, statement.Month)] = statement;
        }
        return Task.CompletedTask;
    }
}
