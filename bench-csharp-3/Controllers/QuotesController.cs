using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;
using PolicyAdmin.Models;
using PolicyAdmin.Security;
using PolicyAdmin.Services;

namespace PolicyAdmin.Controllers;

[ApiController]
[Authorize]
[Route("api/quotes")]
public sealed class QuotesController : ControllerBase
{
    private readonly string _connectionString;
    private readonly QuoteService _quotes;

    public QuotesController(IConfiguration configuration, QuoteService quotes)
    {
        _connectionString = configuration.GetConnectionString("PolicyAdmin")
            ?? throw new InvalidOperationException("Missing PolicyAdmin connection string.");
        _quotes = quotes;
    }

    [HttpGet("{reference}")]
    public async Task<IActionResult> Get(string reference, CancellationToken ct)
    {
        var quote = await LoadAsync(reference, ct);
        if (quote is null || quote.AgentId != User.GetAgentId())
        {
            return NotFound();
        }
        return Ok(quote);
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromQuery] LineOfBusiness line, [FromQuery] decimal baseRate,
        [FromQuery] int priorClaims, CancellationToken ct)
    {
        var quote = _quotes.PriceQuote(User.GetAgentId(), line, baseRate, priorClaims);
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = "INSERT INTO Quotes (Reference, AgentId, LineOfBusiness, QuotedPremium, CreatedUtc) " +
                              "VALUES (@reference, @agentId, @line, @premium, @created)";
        command.Parameters.AddWithValue("@reference", quote.Reference);
        command.Parameters.AddWithValue("@agentId", quote.AgentId);
        command.Parameters.AddWithValue("@line", (int)quote.Line);
        command.Parameters.AddWithValue("@premium", quote.QuotedPremium);
        command.Parameters.AddWithValue("@created", quote.CreatedUtc);
        await command.ExecuteNonQueryAsync(ct);
        return CreatedAtAction(nameof(Get), new { reference = quote.Reference }, quote);
    }

    private async Task<Quote?> LoadAsync(string reference, CancellationToken ct)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);
        await using var command = connection.CreateCommand();
        command.CommandText = "SELECT Reference, AgentId, LineOfBusiness, QuotedPremium, CreatedUtc FROM Quotes WHERE Reference = @reference";
        command.Parameters.AddWithValue("@reference", reference);
        await using var reader = await command.ExecuteReaderAsync(ct);
        if (!await reader.ReadAsync(ct))
        {
            return null;
        }
        return new Quote
        {
            Reference = reader.GetString(0),
            AgentId = reader.GetInt32(1),
            Line = (LineOfBusiness)reader.GetInt32(2),
            QuotedPremium = reader.GetDecimal(3),
            CreatedUtc = reader.GetDateTime(4)
        };
    }
}
