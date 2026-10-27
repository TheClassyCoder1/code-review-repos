using System;
using System.Collections.Generic;
using System.Linq;
using CsharpLarge01.Part5.Domain;

namespace CsharpLarge01.Part5.Scheduling
{
    /// <summary>
    /// Grants a payment holiday by pushing every unsettled installment due on or
    /// after the holiday start back by a number of months. Already-late
    /// installments are left alone so the holiday cannot hide arrears.
    /// </summary>
    public sealed class PaymentHolidayPlanner
    {
        private const int MaxHolidayMonths = 6;

        private readonly HolidayCalendar _calendar;

        public PaymentHolidayPlanner(HolidayCalendar calendar)
        {
            _calendar = calendar ?? throw new ArgumentNullException(nameof(calendar));
        }

        public IReadOnlyList<int> Apply(ServicedLoan loan, DateTime holidayStart, int months)
        {
            if (loan == null)
            {
                throw new ArgumentNullException(nameof(loan));
            }
            if (months < 1 || months > MaxHolidayMonths)
            {
                throw new ArgumentOutOfRangeException(nameof(months), "Holidays last 1 to " + MaxHolidayMonths + " months.");
            }

            var start = holidayStart.Date;
            var affected = loan.UnsettledInstallments
                .Where(i => i.DueDate >= start)
                .OrderBy(i => i.Sequence)
                .ToList();

            var moved = new List<int>(affected.Count);
            foreach (var installment in affected)
            {
                installment.Reschedule(_calendar.NextBusinessDay(installment.DueDate.AddMonths(months)));
                moved.Add(installment.Sequence);
            }
            return moved;
        }
    }
}
