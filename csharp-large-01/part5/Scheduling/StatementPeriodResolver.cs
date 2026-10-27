using System;

namespace CsharpLarge01.Part5.Scheduling
{
    public sealed class StatementPeriod
    {
        public StatementPeriod(DateTime start, DateTime end, DateTime paymentDue)
        {
            Start = start;
            End = end;
            PaymentDue = paymentDue;
        }

        public DateTime Start { get; }

        public DateTime End { get; }

        public DateTime PaymentDue { get; }

        public int LengthInDays => (End - Start).Days + 1;
    }

    /// <summary>
    /// Works out which statement cycle a date belongs to. Borrowers pick a cycle day
    /// (1-31); in shorter months the cycle closes on the last day of the month.
    /// </summary>
    public sealed class StatementPeriodResolver
    {
        private readonly HolidayCalendar _calendar;
        private readonly int _paymentGraceDays;

        public StatementPeriodResolver(HolidayCalendar calendar, int paymentGraceDays)
        {
            if (paymentGraceDays < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(paymentGraceDays));
            }

            _calendar = calendar ?? throw new ArgumentNullException(nameof(calendar));
            _paymentGraceDays = paymentGraceDays;
        }

        public StatementPeriod Resolve(DateTime asOf, int cycleDay)
        {
            if (cycleDay < 1 || cycleDay > 31)
            {
                throw new ArgumentOutOfRangeException(nameof(cycleDay), "Cycle day must be between 1 and 31.");
            }

            var date = asOf.Date;
            var closeThisMonth = ClampToMonth(date.Year, date.Month, cycleDay);

            DateTime periodEnd;
            if (date <= closeThisMonth)
            {
                periodEnd = closeThisMonth;
            }
            else
            {
                var nextMonth = new DateTime(date.Year, date.Month, 1).AddMonths(1);
                periodEnd = ClampToMonth(nextMonth.Year, nextMonth.Month, cycleDay);
            }

            var previousMonth = new DateTime(periodEnd.Year, periodEnd.Month, 1).AddMonths(-1);
            var previousClose = ClampToMonth(previousMonth.Year, previousMonth.Month, cycleDay);
            var periodStart = previousClose.AddDays(1);
            var paymentDue = _calendar.NextBusinessDay(periodEnd.AddDays(_paymentGraceDays));

            return new StatementPeriod(periodStart, periodEnd, paymentDue);
        }

        private static DateTime ClampToMonth(int year, int month, int day)
        {
            return new DateTime(year, month, Math.Min(day, DateTime.DaysInMonth(year, month)));
        }
    }
}
