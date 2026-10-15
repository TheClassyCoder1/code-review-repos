namespace Payroll.Models;

public enum PayRunStatus
{
    Draft,
    Calculated,
    Approved,
    Submitted,
    Paid
}

public enum TimesheetStatus
{
    Open,
    Submitted,
    Approved,
    Rejected
}

public sealed record BankAccount(string RoutingNumber, string AccountNumber)
{
    public override string ToString() =>
        AccountNumber.Length <= 4 ? "****" : "****" + AccountNumber[^4..];
}

public sealed class Employee
{
    public int Id { get; set; }
    public int CompanyId { get; set; }
    public string FullName { get; set; } = "";
    public string Email { get; set; } = "";
    public string Department { get; set; } = "";
    public decimal AnnualSalary { get; set; }
    public string RoutingNumber { get; set; } = "";
    public string AccountNumber { get; set; } = "";

    public BankAccount BankAccount => new(RoutingNumber, AccountNumber);
}

public sealed class PayRun
{
    public Guid Id { get; set; }
    public int CompanyId { get; set; }
    public DateOnly PeriodStart { get; set; }
    public DateOnly PeriodEnd { get; set; }
    public PayRunStatus Status { get; set; }
    public decimal TotalGross { get; set; }
    public DateTime CreatedAt { get; set; }
}

public sealed class PayslipLine
{
    public Guid PayRunId { get; set; }
    public int EmployeeId { get; set; }
    public string Description { get; set; } = "";
    public decimal Amount { get; set; }
}

public sealed class TimesheetEntry
{
    public long Id { get; set; }
    public int EmployeeId { get; set; }
    public DateOnly WorkDate { get; set; }
    public decimal Hours { get; set; }
    public string ProjectCode { get; set; } = "";
    public TimesheetStatus Status { get; set; }
}

public sealed record CreatePayRunRequest(DateOnly PeriodStart, DateOnly PeriodEnd, string? CallbackUrl);

public sealed record BankPayment(int EmployeeId, BankAccount Account, decimal Amount);

public sealed record EmployeeCredentials(int EmployeeId, int CompanyId, string PasswordHash);

public sealed record PayslipShare(Guid PayRunId, int EmployeeId);

public sealed record LoginRequest(string Email, string Password, string? Next);
