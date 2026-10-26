using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;

namespace CsharpLarge01.Part5.Scheduling
{
    /// <summary>
    /// Business-day calendar for a single settlement market. Due dates that fall on
    /// weekends or bank holidays roll forward to the next business day.
    /// </summary>
    public sealed class HolidayCalendar
    {
        private const int MaxConsecutiveClosedDays = 30;
        private const int MaxBusinessDayOffset = 3660;

        private readonly HashSet<DateTime> _holidays;

        public HolidayCalendar(string marketCode, IEnumerable<DateTime> holidays)
        {
            if (string.IsNullOrWhiteSpace(marketCode))
            {
                throw new ArgumentException("Market code is required.", nameof(marketCode));
            }

            MarketCode = marketCode;
            _holidays = new HashSet<DateTime>((holidays ?? Enumerable.Empty<DateTime>()).Select(d => d.Date));
        }

        public string MarketCode { get; }

        public bool IsBusinessDay(DateTime date)
        {
            var day = date.Date;
            if (day.DayOfWeek == DayOfWeek.Saturday || day.DayOfWeek == DayOfWeek.Sunday)
            {
                return false;
            }
            return !_holidays.Contains(day);
        }

        public DateTime NextBusinessDay(DateTime date)
        {
            var candidate = date.Date;
            for (var closed = 0; closed <= MaxConsecutiveClosedDays; closed++)
            {
                if (IsBusinessDay(candidate))
                {
                    return candidate;
                }
                candidate = candidate.AddDays(1);
            }
            throw new InvalidOperationException(
                "Calendar " + MarketCode + " has no business day after " + Format(date) + ".");
        }

        public DateTime AddBusinessDays(DateTime start, int businessDays)
        {
            if (businessDays > MaxBusinessDayOffset || businessDays < -MaxBusinessDayOffset)
            {
                throw new ArgumentOutOfRangeException(nameof(businessDays));
            }

            var current = start.Date;
            if (businessDays == 0)
            {
                return NextBusinessDay(current);
            }

            var step = businessDays > 0 ? 1 : -1;
            var remaining = Math.Abs(businessDays);
            var consecutiveClosed = 0;
            while (remaining > 0)
            {
                current = current.AddDays(step);
                if (IsBusinessDay(current))
                {
                    remaining--;
                    consecutiveClosed = 0;
                }
                else if (++consecutiveClosed > MaxConsecutiveClosedDays)
                {
                    throw new InvalidOperationException(
                        "Calendar " + MarketCode + " is closed for over " + MaxConsecutiveClosedDays + " days near " + Format(current) + ".");
                }
            }
            return current;
        }

        private static string Format(DateTime date)
        {
            return date.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
        }
    }
}
