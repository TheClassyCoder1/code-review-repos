using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace LedgerApi.Controllers;

[ApiController]
[Authorize(Policy = "Ops")]
public abstract class OpsControllerBase : ControllerBase
{
}
