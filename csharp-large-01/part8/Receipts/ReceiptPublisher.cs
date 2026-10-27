using System.Net.Http.Json;

namespace Lending.Part8.Receipts;

public sealed record PaymentReceipt(Guid ReceiptId, string LoanId, decimal Amount, DateTimeOffset PostedAt);

public sealed class ReceiptPublisher(HttpClient http)
{
    public async Task<bool> TryPublishAsync(PaymentReceipt receipt, CancellationToken ct)
    {
        try
        {
            using var response = await http.PostAsJsonAsync("receipts", receipt, ct);
            return response.IsSuccessStatusCode;
        }
        catch (Exception)
        {
            return false;
        }
    }
}
