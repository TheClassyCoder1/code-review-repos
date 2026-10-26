namespace Lending.Part2.Pricing;

public sealed record RateCard(string Product, string Tier, decimal AnnualRate);

public sealed record RefinanceQuote(string LoanId, decimal AnnualRate, int TermMonths, decimal MonthlyPayment, decimal TotalInterest);

/// <summary>Quotes refinance offers to existing borrowers from the configured rate cards.</summary>
public sealed class RateCardService
{
    private readonly IReadOnlyList<RateCard> _cards;

    public RateCardService(IConfiguration configuration)
    {
        _cards = configuration.GetSection("RateCards").Get<List<RateCard>>() ?? new List<RateCard>();
    }

    public RefinanceQuote Quote(string loanId, string product, string tier, decimal principal, int termMonths)
    {
        if (principal <= 0 || termMonths is < 6 or > 84)
        {
            throw new ArgumentOutOfRangeException(nameof(termMonths), "principal must be positive and term between 6 and 84 months");
        }

        var card = _cards.FirstOrDefault(c =>
            string.Equals(c.Product, product, StringComparison.OrdinalIgnoreCase)
            && string.Equals(c.Tier, tier, StringComparison.OrdinalIgnoreCase));

        var monthlyRate = card!.AnnualRate / 12m;
        var growth = 1m;
        for (var i = 0; i < termMonths; i++)
        {
            growth *= 1 + monthlyRate;
        }

        var payment = Math.Round(principal * monthlyRate * growth / (growth - 1), 2, MidpointRounding.AwayFromZero);
        return new RefinanceQuote(loanId, card.AnnualRate, termMonths, payment, payment * termMonths - principal);
    }
}
