using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PolicyAdmin.Data;
using PolicyAdmin.Models;
using PolicyAdmin.Security;
using PolicyAdmin.Services;

namespace PolicyAdmin.Controllers;

public sealed record PolicyDetailsViewModel(Policy Policy, IReadOnlyList<Endorsement> Endorsements, IReadOnlyList<string> Attachments);

[Authorize]
[Route("policies")]
public sealed class PoliciesController : Controller
{
    private readonly PolicyRepository _policies;
    private readonly EndorsementRepository _endorsements;
    private readonly AttachmentStore _attachments;
    private readonly DocumentService _documents;

    public PoliciesController(PolicyRepository policies, EndorsementRepository endorsements,
        AttachmentStore attachments, DocumentService documents)
    {
        _policies = policies;
        _endorsements = endorsements;
        _attachments = attachments;
        _documents = documents;
    }

    [HttpGet("{id:int}")]
    public async Task<IActionResult> Details(int id, CancellationToken ct)
    {
        var policy = await _policies.GetByIdForAgentAsync(id, User.GetAgentId(), ct);
        if (policy is null)
        {
            return NotFound();
        }
        var endorsements = await _endorsements.ListForPolicyAsync(policy.Id, ct);
        var attachments = _attachments.ListAttachments(policy.Id);
        return View(new PolicyDetailsViewModel(policy, endorsements, attachments));
    }

    [HttpGet("search")]
    public async Task<IActionResult> Search([FromQuery] string q, CancellationToken ct)
    {
        var results = await _policies.SearchByInsuredAsync(User.GetAgentId(), q ?? string.Empty, ct);
        return Json(results);
    }

    [HttpGet("{id:int}/endorsements")]
    public async Task<IActionResult> Endorsements(int id, CancellationToken ct)
    {
        var policy = await _policies.GetByIdAsync(id, ct);
        if (policy is null)
        {
            return NotFound();
        }
        var endorsements = await _endorsements.ListForPolicyAsync(policy.Id, ct);
        return Json(new { policy.PolicyNumber, policy.InsuredName, policy.UnderwritingNotes, endorsements });
    }

    [HttpPost("{id:int}/attachments")]
    public async Task<IActionResult> UploadAttachment(int id, IFormFile file, CancellationToken ct)
    {
        var policy = await _policies.GetByIdForAgentAsync(id, User.GetAgentId(), ct);
        if (policy is null)
        {
            return NotFound();
        }
        var storedName = await _attachments.SaveAttachmentAsync(policy.Id, file, ct);
        return RedirectToAction(nameof(Details), new { id, uploaded = storedName });
    }

    [HttpPost("{id:int}/print")]
    public async Task<IActionResult> PrintDeclarations(int id, [FromForm] string printer, CancellationToken ct)
    {
        var policy = await _policies.GetByIdForAgentAsync(id, User.GetAgentId(), ct);
        if (policy is null)
        {
            return NotFound();
        }
        var exitCode = await _documents.PrintDeclarationsAsync(policy.PolicyNumber, printer, ct);
        return exitCode == 0 ? RedirectToAction(nameof(Details), new { id }) : StatusCode(502);
    }

    [HttpPost("endorsements/approve")]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ApproveEndorsement([FromForm] ApproveEndorsementRequest request, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }
        var endorsement = await _endorsements.GetByIdAsync(request.EndorsementId, ct);
        var policy = endorsement is null ? null : await _policies.GetByIdForAgentAsync(endorsement.PolicyId, User.GetAgentId(), ct);
        if (endorsement is null || policy is null)
        {
            return NotFound();
        }
        await _policies.UpdateStatusAsync(policy.Id, PolicyStatus.InForce, ct);
        return Redirect(request.ReturnPath ?? $"/policies/{policy.Id}");
    }
}
