namespace Lending.Part2.Reporting;

public sealed record DelinquencyReportQuery(int MinDaysPastDue, string? BranchCode, string? Sort, string? Direction, int Page, int PageSize);

public sealed class DelinquencyReportService(DelinquencyReportRepository repository, ReportSortCatalog sorts)
{
    public Task<IReadOnlyList<DelinquencyRow>> GetPageAsync(DelinquencyReportQuery query, CancellationToken ct)
    {
        var pageSize = Math.Clamp(query.PageSize, 1, 500);
        var offset = (Math.Max(query.Page, 1) - 1) * pageSize;
        var sort = sorts.ForDelinquency(query.Sort, query.Direction);
        var branch = string.IsNullOrWhiteSpace(query.BranchCode) ? null : query.BranchCode.Trim();

        return repository.QueryAsync(Math.Max(query.MinDaysPastDue, 1), branch, sort.Column, sort.Direction, pageSize, offset, ct);
    }
}
