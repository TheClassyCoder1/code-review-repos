using PolicyAdmin.Data;
using PolicyAdmin.Models;
using PolicyAdmin.Security;
using PolicyAdmin.Services;

namespace PolicyAdmin.Endpoints;

public static class PolicyEndpoints
{
    private static readonly HashSet<string> DocumentTemplates = new(StringComparer.Ordinal)
    {
        "declarations",
        "id-card",
        "cancellation-notice",
        "renewal-offer"
    };

    public static void MapPolicyEndpoints(this WebApplication app)
    {
        var policy = app.MapGroup("/api/policies/{policyId:int}")
            .RequireAuthorization()
            .AddEndpointFilter<AgentOwnsPolicyFilter>();
        policy.MapGet("/premium", GetPremiumBreakdownAsync);
        policy.MapPost("/payments", RecordPaymentAsync);
        policy.MapPost("/supporting-documents", FetchSupportingDocumentAsync);
        policy.MapPost("/carrier-binding", SubmitCarrierBindingAsync);

        var api = app.MapGroup("/api").RequireAuthorization();
        api.MapGet("/rates/{line}/{state}", GetBaseRatesAsync);
        api.MapPost("/documents/{policyNumber:regex(^PA-[0-9]{{8}}$)}/render", RenderDeclarationsAsync);
        api.MapGet("/templates/{name}", GetTemplateAsync);
        api.MapPost("/endorsements/import/typed", ImportTypedChange);
        api.MapPost("/endorsements/import/legacy", ImportLegacyBatch);

        app.MapGet("/agency/switch", ContinueAfterAgencySwitch).RequireAuthorization();
    }

    private static async Task<IResult> GetPremiumBreakdownAsync(int policyId, PolicyRepository policies,
        EndorsementRepository endorsements, CancellationToken ct)
    {
        var policy = await policies.GetByIdAsync(policyId, ct);
        if (policy is null)
        {
            return Results.NotFound();
        }
        var items = await endorsements.ListForPolicyAsync(policy.Id, ct);
        return Results.Ok(PremiumCalculator.Breakdown(policy, items));
    }

    private static async Task<IResult> RecordPaymentAsync(int policyId, PremiumPayment payment, PaymentService payments,
        CancellationToken ct)
    {
        payment.PolicyId = policyId;
        var id = await payments.RecordPremiumPaymentAsync(payment, ct);
        return Results.Created($"/api/policies/{policyId}/payments/{id}", new { id });
    }

    private static async Task<IResult> FetchSupportingDocumentAsync(int policyId, string documentUrl, CarrierClient carriers,
        AttachmentStore attachments, CancellationToken ct)
    {
        var bytes = await carriers.FetchSupportingDocumentAsync(documentUrl, ct);
        return Results.Ok(new { policyId, size = CarrierClient.DescribeSize(bytes.Length), existing = attachments.ListAttachments(policyId).Count });
    }

    private static async Task<IResult> SubmitCarrierBindingAsync(int policyId, CarrierSubmission submission,
        CarrierClient carriers, PolicyRepository policies, CancellationToken ct)
    {
        var accepted = await carriers.SubmitBindingAsync(submission, ct);
        if (accepted)
        {
            await policies.UpdateStatusAsync(policyId, PolicyStatus.Bound, ct);
        }
        return accepted ? Results.Accepted() : Results.StatusCode(502);
    }

    private static async Task<IResult> GetBaseRatesAsync(string line, string state, PolicyRepository policies, CancellationToken ct)
    {
        if (!Enum.TryParse<LineOfBusiness>(line, ignoreCase: true, out var lob) || !Enum.IsDefined(lob) || state.Length != 2)
        {
            return Results.BadRequest();
        }
        var rates = await policies.GetBaseRatesAsync(lob.ToString(), state.ToUpperInvariant(), ct);
        return Results.Ok(rates);
    }

    private static async Task<IResult> RenderDeclarationsAsync(string policyNumber, DocumentService documents,
        AttachmentStore attachments, CancellationToken ct)
    {
        var template = await attachments.ReadTemplateAsync("declarations", ct);
        var htmlPath = Path.Combine(Path.GetTempPath(), $"{policyNumber}.html");
        await File.WriteAllTextAsync(htmlPath, template.Replace("{{policy_number}}", policyNumber), ct);
        var pdfPath = await documents.RenderDeclarationsPdfAsync(policyNumber, htmlPath, ct);
        return Results.File(pdfPath, "application/pdf");
    }

    private static async Task<IResult> GetTemplateAsync(string name, AttachmentStore attachments, CancellationToken ct)
    {
        if (!DocumentTemplates.Contains(name))
        {
            return Results.NotFound();
        }
        var html = await attachments.ReadTemplateAsync(name, ct);
        return Results.Content(html, "text/html");
    }

    private static IResult ImportTypedChange(EndorsementImportEnvelope envelope, EndorsementImportService imports)
    {
        if (!EndorsementTypeRegistry.IsAllowed(envelope.TypeName))
        {
            return Results.BadRequest(new { error = "Unsupported endorsement type." });
        }
        var change = imports.ReadTypedChange(envelope);
        return Results.Ok(change);
    }

    private static IResult ImportLegacyBatch(HttpRequest request, EndorsementImportService imports)
    {
        var batch = imports.ReadLegacyBatch(request.Body);
        return Results.Ok(new { imported = batch.Count });
    }

    private static IResult ContinueAfterAgencySwitch(HttpContext context, int agencyId, string? next)
    {
        context.Response.Cookies.Append("active_agency", agencyId.ToString(), new CookieOptions
        {
            HttpOnly = true,
            Secure = true,
            SameSite = SameSiteMode.Lax
        });
        return Results.Redirect(string.IsNullOrEmpty(next) ? "/policies" : next);
    }
}
