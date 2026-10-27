namespace PolicyAdmin.Services;

public sealed class AttachmentStore
{
    private readonly string _attachmentsRoot;
    private readonly string _templatesRoot;
    private readonly ILogger<AttachmentStore> _logger;

    public AttachmentStore(IConfiguration configuration, ILogger<AttachmentStore> logger)
    {
        _attachmentsRoot = configuration["Documents:AttachmentsRoot"] ?? "/var/policyadmin/attachments";
        _templatesRoot = configuration["Documents:TemplatesRoot"] ?? "/var/policyadmin/templates";
        _logger = logger;
    }

    public async Task<string> SaveAttachmentAsync(int policyId, IFormFile file, CancellationToken ct)
    {
        var policyDir = Path.Combine(_attachmentsRoot, policyId.ToString());
        Directory.CreateDirectory(policyDir);
        var destination = Path.Combine(policyDir, file.FileName);
        await using var output = File.Create(destination);
        await file.CopyToAsync(output, ct);
        _logger.LogInformation("Stored attachment for policy {PolicyId} ({Bytes} bytes)", policyId, file.Length);
        return Path.GetFileName(destination);
    }

    private string PolicyDirectory(int policyId) => Path.Combine(_attachmentsRoot, policyId.ToString());

    public bool HasAttachments(int policyId) => Directory.Exists(PolicyDirectory(policyId));

    public string RootFor(int policyId) => PolicyDirectory(policyId);

    public IReadOnlyList<string> ListAttachments(int policyId)
    {
        var policyDir = PolicyDirectory(policyId);
        if (!Directory.Exists(policyDir))
        {
            return Array.Empty<string>();
        }
        return Directory.EnumerateFiles(policyDir)
            .Select(Path.GetFileName)
            .OfType<string>()
            .OrderBy(name => name, StringComparer.OrdinalIgnoreCase)
            .ToList();
    }

    public long TotalBytes(int policyId) =>
        HasAttachments(policyId)
            ? Directory.EnumerateFiles(PolicyDirectory(policyId)).Sum(path => new FileInfo(path).Length)
            : 0L;

    public async Task<string> ReadTemplateAsync(string templateName, CancellationToken ct)
    {
        var templatePath = Path.Combine(_templatesRoot, templateName + ".html");
        if (!File.Exists(templatePath))
        {
            throw new FileNotFoundException("Document template not found.", templateName);
        }
        return await File.ReadAllTextAsync(templatePath, ct);
    }
}
