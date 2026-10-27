namespace Lending.Part8.Intake;

public sealed record IntakeResult(Guid DocumentId, bool HasThumbnail);

public sealed class DocumentIntakeService(
    StagedDocumentStore store,
    ThumbnailRenderer thumbnails,
    ILogger<DocumentIntakeService> logger)
{
    private const long MaxBytes = 20 * 1024 * 1024;

    public async Task<IntakeResult?> IngestAsync(IFormFile upload, CancellationToken ct)
    {
        if (upload.Length == 0 || upload.Length > MaxBytes
            || !string.Equals(upload.ContentType, "application/pdf", StringComparison.OrdinalIgnoreCase))
        {
            return null;
        }

        await using var content = upload.OpenReadStream();
        var staged = await store.SaveAsync(content, ct);
        var thumbnail = await thumbnails.RenderFirstPageAsync(staged.Path, ct);

        logger.LogInformation("Staged document {DocumentId} ({Length} bytes)", staged.Id, staged.Length);
        return new IntakeResult(staged.Id, thumbnail is not null);
    }
}
