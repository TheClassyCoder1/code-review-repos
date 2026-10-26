using System;
using System.Collections.Generic;
using System.Linq;

namespace CsharpLarge01.Part5.Domain
{
    /// <summary>
    /// Servicing-side view of a disbursed loan: the repayment schedule plus the
    /// balances collections needs. Borrower identity stays in loan-service; only
    /// the internal loan id is held here.
    /// </summary>
    public sealed class ServicedLoan
    {
        private readonly List<Installment> _installments;

        public ServicedLoan(string loanId, string currencyCode, decimal originalPrincipal, decimal annualRate,
            DateTime disbursedOn, IEnumerable<Installment> installments)
        {
            if (string.IsNullOrWhiteSpace(loanId))
            {
                throw new ArgumentException("Loan id is required.", nameof(loanId));
            }
            if (string.IsNullOrWhiteSpace(currencyCode) || currencyCode.Length != 3)
            {
                throw new ArgumentException("Currency must be an ISO 4217 code.", nameof(currencyCode));
            }
            if (installments == null)
            {
                throw new ArgumentNullException(nameof(installments));
            }

            LoanId = loanId;
            CurrencyCode = currencyCode.ToUpperInvariant();
            OriginalPrincipal = originalPrincipal;
            AnnualRate = annualRate;
            DisbursedOn = disbursedOn.Date;
            _installments = installments.OrderBy(i => i.Sequence).ToList();
        }

        public string LoanId { get; }

        public string CurrencyCode { get; }

        public decimal OriginalPrincipal { get; }

        public decimal AnnualRate { get; }

        public DateTime DisbursedOn { get; }

        public IReadOnlyList<Installment> Installments => _installments;

        public decimal OutstandingPrincipal => _installments
            .Where(i => i.Status != InstallmentStatus.Waived)
            .Sum(i => i.OutstandingPrincipal);

        public decimal OutstandingFees => _installments
            .Where(i => i.Status != InstallmentStatus.Waived)
            .Sum(i => i.OutstandingFees);

        public IEnumerable<Installment> UnsettledInstallments => _installments.Where(i => !i.IsSettled);
    }

    public sealed class PaymentReceipt
    {
        public PaymentReceipt(string paymentId, string loanId, decimal amount, DateTime receivedOn)
        {
            if (amount <= 0m)
            {
                throw new ArgumentOutOfRangeException(nameof(amount), "Payments must be positive.");
            }

            PaymentId = paymentId ?? throw new ArgumentNullException(nameof(paymentId));
            LoanId = loanId ?? throw new ArgumentNullException(nameof(loanId));
            Amount = amount;
            ReceivedOn = receivedOn;
        }

        public string PaymentId { get; }

        public string LoanId { get; }

        public decimal Amount { get; }

        public DateTime ReceivedOn { get; }
    }

    public sealed class AllocationLine
    {
        public AllocationLine(int installmentSequence, decimal fees, decimal interest, decimal principal)
        {
            InstallmentSequence = installmentSequence;
            Fees = fees;
            Interest = interest;
            Principal = principal;
        }

        public int InstallmentSequence { get; }

        public decimal Fees { get; }

        public decimal Interest { get; }

        public decimal Principal { get; }

        public decimal Total => Fees + Interest + Principal;
    }

    public sealed class AllocationResult
    {
        public AllocationResult(string paymentId, IReadOnlyList<AllocationLine> lines, decimal unapplied)
        {
            PaymentId = paymentId;
            Lines = lines;
            Unapplied = unapplied;
        }

        public string PaymentId { get; }

        public IReadOnlyList<AllocationLine> Lines { get; }

        public decimal Unapplied { get; }

        public decimal Applied => Lines.Sum(l => l.Total);
    }

    public enum DelinquencyBucket
    {
        Current,
        Days1To30,
        Days31To60,
        Days61To90,
        Over90
    }

    public sealed class DelinquencyStatus
    {
        public DelinquencyStatus(string loanId, int daysPastDue, decimal amountPastDue, DelinquencyBucket bucket)
        {
            LoanId = loanId;
            DaysPastDue = daysPastDue;
            AmountPastDue = amountPastDue;
            Bucket = bucket;
        }

        public string LoanId { get; }

        public int DaysPastDue { get; }

        public decimal AmountPastDue { get; }

        public DelinquencyBucket Bucket { get; }
    }
}
