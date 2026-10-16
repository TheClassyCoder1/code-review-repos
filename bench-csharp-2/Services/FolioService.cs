using System.Globalization;
using Hotel.Data;
using Hotel.Models;
using Microsoft.EntityFrameworkCore;

namespace Hotel.Services;

public class FolioService
{
    private readonly HotelDbContext _db;
    private readonly PaymentGatewayClient _payments;
    private readonly ILogger<FolioService> _logger;
    private readonly string _invoiceRoot;

    public FolioService(HotelDbContext db, PaymentGatewayClient payments, IConfiguration config, ILogger<FolioService> logger)
    {
        _db = db;
        _payments = payments;
        _logger = logger;
        _invoiceRoot = config["Invoices:Root"] ?? "/var/lib/hotel/invoices";
    }

    public async Task<decimal> PostChargeAsync(int folioId, PostChargeRequest request, CancellationToken ct)
    {
        var folio = await _db.Folios.AsNoTracking().SingleAsync(f => f.Id == folioId, ct);
        if (folio.Closed)
        {
            throw new InvalidOperationException("Folio is closed.");
        }

        var newBalance = folio.Balance + request.Amount;
        _db.FolioLines.Add(new FolioLine { FolioId = folioId, Description = request.Description, Amount = request.Amount, PostedAt = DateTime.UtcNow });

        await using var tx = await _db.Database.BeginTransactionAsync(ct);
        await _db.SaveChangesAsync(ct);
        await _db.Folios
            .Where(f => f.Id == folioId)
            .ExecuteUpdateAsync(s => s.SetProperty(f => f.Balance, newBalance), ct);
        await tx.CommitAsync(ct);
        return newBalance;
    }

    public async Task<Folio?> GetFolioWithLinesAsync(int hotelId, int folioId, CancellationToken ct)
    {
        return await _db.Folios
            .AsNoTracking()
            .Include(f => f.Lines)
            .SingleOrDefaultAsync(f => f.Id == folioId && f.Booking.HotelId == hotelId, ct);
    }

    public async Task<bool> SettleAsync(int folioId, string paymentToken, CancellationToken ct)
    {
        var folio = await _db.Folios.SingleAsync(f => f.Id == folioId, ct);
        if (folio.Closed)
        {
            return true;
        }

        try
        {
            await _payments.CaptureAsync(paymentToken, folio.Balance, folio.Currency, $"folio-{folio.Id}", ct);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Payment capture failed for folio {FolioId}", folio.Id);
        }

        folio.Closed = true;
        await _db.SaveChangesAsync(ct);
        return true;
    }

    public async Task<bool> BelongsToHotelAsync(int hotelId, int folioId, CancellationToken ct)
    {
        return await _db.Folios
            .AsNoTracking()
            .AnyAsync(f => f.Id == folioId && f.Booking.HotelId == hotelId, ct);
    }

    public static FolioStatement ComposeStatement(Task<Folio> folioTask, Task<decimal> fxRateTask)
    {
        var folio = folioTask.Result;
        var rate = fxRateTask.Result;
        var lines = folio.Lines.OrderBy(l => l.PostedAt).ToList();
        var homeBalance = Math.Round(folio.Balance * rate, 2, MidpointRounding.AwayFromZero);
        return new FolioStatement(folio.Id, folio.Currency, folio.Balance, homeBalance, lines);
    }

    private string InvoicePath(int hotelId, int folioId)
    {
        return Path.Combine(_invoiceRoot, hotelId.ToString(CultureInfo.InvariantCulture), $"{folioId}.pdf");
    }

    public Stream? OpenInvoicePdf(int hotelId, int folioId)
    {
        var path = InvoicePath(hotelId, folioId);
        if (!File.Exists(path))
        {
            return null;
        }
        var stream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read, 81920, useAsync: true);
        return stream;
    }

    public async Task<int> CountOpenFoliosAsync(int hotelId, CancellationToken ct)
    {
        return await _db.Folios
            .AsNoTracking()
            .CountAsync(f => !f.Closed && f.Booking.HotelId == hotelId, ct);
    }

    public static decimal CalculateBalance(IEnumerable<FolioLine> lines)
    {
        var total = 0m;
        foreach (var line in lines)
        {
            total += line.Amount;
        }
        return Math.Round(total, 2, MidpointRounding.ToEven);
    }
}
