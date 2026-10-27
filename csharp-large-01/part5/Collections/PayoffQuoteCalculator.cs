using System;
using CsharpLarge01.Part5.Domain;

namespace CsharpLarge01.Part5.Collections
{
    public sealed class PayoffQuote
    {
        public PayoffQuote(string loanId, DateTime payoffDate, decimal principal, decimal interest, decimal fees)
        {
            LoanId = loanId;
            PayoffDate = payoffDate;
            Principal = principal;
            Interest = interest;
            Fees = fees;
        }

        public string LoanId { get; }

        public DateTime PayoffDate { get; }

        public decimal Principal { get; }

        public decimal Interest { get; }

        public decimal Fees { get; }

        public decimal Total => Principal + Interest + Fees;
    }

    /// <summary>
    /// Quotes the amount needed to close a loan on a given date: all remaining
    /// principal, interest already billed but unpaid, interest accrued since the
    /// last due date, and unpaid fees. Interest scheduled on future installments is
    /// not charged because it has not been earned yet.
    /// </summary>
    public sealed class PayoffQuoteCalculator
    {
        private readonly InterestAccrualCalculator _accrual;
        private readonly DayCountConvention _convention;

        public PayoffQuoteCalculator(InterestAccrualCalculator accrual, DayCountConvention convention)
        {
            _accrual = accrual ?? throw new ArgumentNullException(nameof(accrual));
            _convention = convention;
        }

        public PayoffQuote Quote(ServicedLoan loan, DateTime payoffDate)
        {
            if (loan == null)
            {
                throw new ArgumentNullException(nameof(loan));
            }

            var date = payoffDate.Date;
            if (date < loan.DisbursedOn)
            {
                throw new ArgumentOutOfRangeException(nameof(payoffDate), "Payoff date precedes disbursement.");
            }

            var lastDueDate = loan.DisbursedOn;
            var billedInterest = 0m;
            foreach (var installment in loan.Installments)
            {
                if (installment.Status == InstallmentStatus.Waived || installment.DueDate > date)
                {
                    continue;
                }

                billedInterest += installment.OutstandingInterest;
                if (installment.DueDate > lastDueDate)
                {
                    lastDueDate = installment.DueDate;
                }
            }

            var principal = loan.OutstandingPrincipal;
            var accrued = _accrual.Accrue(principal, loan.AnnualRate, lastDueDate, date, _convention);
            return new PayoffQuote(loan.LoanId, date, principal, billedInterest + accrued, loan.OutstandingFees);
        }
    }
}
