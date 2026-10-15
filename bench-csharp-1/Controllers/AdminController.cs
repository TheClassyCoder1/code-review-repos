using Microsoft.AspNetCore.Mvc;
using Payroll.Data;
using Payroll.Security;

namespace Payroll.Controllers;

[ApiController]
[Route("admin/employees")]
public sealed class AdminController : ControllerBase
{
    private readonly EmployeeRepository _employees;

    public AdminController(EmployeeRepository employees)
    {
        _employees = employees;
    }

    [HttpGet("{employeeId:int}/bank-details")]
    public async Task<IActionResult> GetBankDetails(int employeeId)
    {
        var employee = await _employees.GetByIdAsync(User.GetCompanyId(), employeeId);
        if (employee is null)
        {
            return NotFound();
        }
        return Ok(new
        {
            employee_id = employee.Id,
            routing_number = employee.RoutingNumber,
            account_number = employee.AccountNumber
        });
    }
}
