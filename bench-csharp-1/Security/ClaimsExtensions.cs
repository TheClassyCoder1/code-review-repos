using System.Security.Claims;

namespace Payroll.Security;

public static class ClaimsExtensions
{
    public const string EmployeeIdClaim = "employee_id";
    public const string CompanyIdClaim = "company_id";

    public static int GetEmployeeId(this ClaimsPrincipal user) =>
        int.Parse(user.FindFirst(EmployeeIdClaim)?.Value ?? throw new InvalidOperationException("employee_id claim missing"));

    public static int GetCompanyId(this ClaimsPrincipal user) =>
        int.Parse(user.FindFirst(CompanyIdClaim)?.Value ?? throw new InvalidOperationException("company_id claim missing"));
}
