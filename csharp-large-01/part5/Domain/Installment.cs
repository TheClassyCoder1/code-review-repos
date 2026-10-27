using System;

namespace CsharpLarge01.Part5.Domain
{
    public enum InstallmentStatus
    {
        Scheduled,
        PartiallyPaid,
        Paid,
        Waived
    }

    /// <summary>
    /// One scheduled repayment of a loan. Amounts are tracked per component so the
    /// payment waterfall can settle fees, interest and principal independently.
    /// </summary>
    public sealed class Installment
    {
        public Installment(int sequence, DateTime dueDate, decimal principalDue, decimal interestDue)
        {
            if (sequence <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(sequence), "Sequence starts at 1.");
            }
            if (principalDue < 0m)
            {
                throw new ArgumentOutOfRangeException(nameof(principalDue));
            }
            if (interestDue < 0m)
            {
                throw new ArgumentOutOfRangeException(nameof(interestDue));
            }

            Sequence = sequence;
            DueDate = dueDate.Date;
            PrincipalDue = principalDue;
            InterestDue = interestDue;
            Status = InstallmentStatus.Scheduled;
        }

        public int Sequence { get; }

        public DateTime DueDate { get; private set; }

        public decimal PrincipalDue { get; }

        public decimal InterestDue { get; }

        public decimal FeesDue { get; private set; }

        public decimal PrincipalPaid { get; private set; }

        public decimal InterestPaid { get; private set; }

        public decimal FeesPaid { get; private set; }

        public bool LateFeeAssessed { get; private set; }

        public InstallmentStatus Status { get; private set; }

        public decimal OutstandingPrincipal => PrincipalDue - PrincipalPaid;

        public decimal OutstandingInterest => InterestDue - InterestPaid;

        public decimal OutstandingFees => FeesDue - FeesPaid;

        public decimal TotalOutstanding =>
            Status == InstallmentStatus.Waived ? 0m : OutstandingPrincipal + OutstandingInterest + OutstandingFees;

        public bool IsSettled => Status == InstallmentStatus.Paid || Status == InstallmentStatus.Waived;

        public void AssessLateFee(decimal amount)
        {
            if (amount <= 0m)
            {
                throw new ArgumentOutOfRangeException(nameof(amount));
            }
            if (LateFeeAssessed)
            {
                throw new InvalidOperationException("A late fee has already been assessed on installment " + Sequence + ".");
            }

            FeesDue += amount;
            LateFeeAssessed = true;
            RefreshStatus();
        }

        public void ApplyPayment(decimal fees, decimal interest, decimal principal)
        {
            if (fees < 0m || interest < 0m || principal < 0m)
            {
                throw new ArgumentOutOfRangeException(nameof(fees), "Payment components must be non-negative.");
            }
            if (fees > OutstandingFees || interest > OutstandingInterest || principal > OutstandingPrincipal)
            {
                throw new InvalidOperationException("Payment exceeds the outstanding amount on installment " + Sequence + ".");
            }

            FeesPaid += fees;
            InterestPaid += interest;
            PrincipalPaid += principal;
            RefreshStatus();
        }

        public void Reschedule(DateTime newDueDate)
        {
            if (IsSettled)
            {
                throw new InvalidOperationException("Settled installments cannot be rescheduled.");
            }
            DueDate = newDueDate.Date;
        }

        public void Waive()
        {
            Status = InstallmentStatus.Waived;
        }

        private void RefreshStatus()
        {
            if (Status == InstallmentStatus.Waived)
            {
                return;
            }

            var paidAnything = FeesPaid + InterestPaid + PrincipalPaid > 0m;
            if (OutstandingPrincipal + OutstandingInterest + OutstandingFees == 0m)
            {
                Status = InstallmentStatus.Paid;
            }
            else
            {
                Status = paidAnything ? InstallmentStatus.PartiallyPaid : InstallmentStatus.Scheduled;
            }
        }
    }
}
