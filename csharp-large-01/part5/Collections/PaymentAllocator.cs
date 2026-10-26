using System;
using System.Collections.Generic;
using System.Linq;
using CsharpLarge01.Part5.Domain;

namespace CsharpLarge01.Part5.Collections
{
    /// <summary>
    /// Applies an incoming payment to a loan using the standard waterfall:
    /// oldest installment first, and within an installment fees, then interest,
    /// then principal. Anything left over is reported as unapplied so treasury can
    /// hold it as a credit balance instead of silently prepaying the loan.
    /// </summary>
    public sealed class PaymentAllocator
    {
        private readonly int _prepaymentWindowDays;

        public PaymentAllocator(int prepaymentWindowDays)
        {
            if (prepaymentWindowDays < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(prepaymentWindowDays));
            }
            _prepaymentWindowDays = prepaymentWindowDays;
        }

        public AllocationResult Allocate(ServicedLoan loan, PaymentReceipt payment)
        {
            if (loan == null)
            {
                throw new ArgumentNullException(nameof(loan));
            }
            if (payment == null)
            {
                throw new ArgumentNullException(nameof(payment));
            }
            if (!string.Equals(loan.LoanId, payment.LoanId, StringComparison.Ordinal))
            {
                throw new ArgumentException("Payment " + payment.PaymentId + " belongs to a different loan.", nameof(payment));
            }

            var remaining = payment.Amount;
            var horizon = payment.ReceivedOn.Date.AddDays(_prepaymentWindowDays);
            var lines = new List<AllocationLine>();
            var ordered = loan.UnsettledInstallments.OrderBy(i => i.DueDate).ThenBy(i => i.Sequence).ToList();

            foreach (var installment in ordered)
            {
                if (remaining <= 0m || installment.DueDate > horizon)
                {
                    break;
                }

                var fees = Math.Min(remaining, installment.OutstandingFees);
                remaining -= fees;
                var interest = Math.Min(remaining, installment.OutstandingInterest);
                remaining -= interest;
                var principal = Math.Min(remaining, installment.OutstandingPrincipal);
                remaining -= principal;

                if (fees + interest + principal == 0m)
                {
                    continue;
                }

                installment.ApplyPayment(fees, interest, principal);
                lines.Add(new AllocationLine(installment.Sequence, fees, interest, principal));
            }

            return new AllocationResult(payment.PaymentId, lines, remaining);
        }
    }
}
