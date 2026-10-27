namespace Payroll.Services;

public sealed class PayslipStore
{
    private readonly string _root;

    public PayslipStore(IConfiguration configuration)
    {
        _root = configuration["Payroll:PayslipRoot"] ?? "/var/lib/payroll/payslips";
    }

    public async Task<byte[]?> ReadAsync(string relativePath, CancellationToken ct)
    {
        var fullPath = Path.Combine(_root, relativePath);
        if (!File.Exists(fullPath))
        {
            return null;
        }
        return await File.ReadAllBytesAsync(fullPath, ct);
    }

    public async Task WriteAsync(int employeeId, Guid payRunId, byte[] pdf, CancellationToken ct)
    {
        var directory = Path.Combine(_root, employeeId.ToString());
        Directory.CreateDirectory(directory);
        await File.WriteAllBytesAsync(Path.Combine(directory, $"{payRunId:N}.pdf"), pdf, ct);
    }

    public IReadOnlyList<string> ListForEmployee(int employeeId)
    {
        var directory = Path.Combine(_root, employeeId.ToString());
        if (!Directory.Exists(directory))
        {
            return Array.Empty<string>();
        }
        return Directory.EnumerateFiles(directory, "*.pdf")
            .Select(file => Path.GetFileNameWithoutExtension(file))
            .OfType<string>()
            .OrderByDescending(name => name)
            .ToList();
    }

    public string RootDirectory => _root;
}
