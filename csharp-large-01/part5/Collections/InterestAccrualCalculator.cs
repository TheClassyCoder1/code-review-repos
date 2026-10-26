using System;

namespace CsharpLarge01.Part5.Collections
{
    public enum DayCountConvention
    {
        Actual365,
        Actual360,
        Thirty360European,
        ActualActualIsda
    }

    /// <summary>
    /// Simple-interest accrual between two dates. The period is (from, to]: interest
    /// for the start date was already accrued by the previous period.
    /// The year fraction is computed exactly and the result rounded once, so
    /// splitting a period never loses or gains pennies relative to daily rounding.
    /// </summary>
    public sealed class InterestAccrualCalculator
    {
        public decimal Accrue(decimal principal, decimal annualRate, DateTime fromExclusive, DateTime toInclusive,
            DayCountConvention convention)
        {
            if (principal < 0m)
            {
                throw new ArgumentOutOfRangeException(nameof(principal));
            }
            if (annualRate < 0m)
            {
                throw new ArgumentOutOfRangeException(nameof(annualRate));
            }

            var start = fromExclusive.Date;
            var end = toInclusive.Date;
            if (end <= start || principal == 0m || annualRate == 0m)
            {
                return 0m;
            }

            decimal yearFraction;
            switch (convention)
            {
                case DayCountConvention.Actual365:
                    yearFraction = (end - start).Days / 365m;
                    break;
                case DayCountConvention.Actual360:
                    yearFraction = (end - start).Days / 360m;
                    break;
                case DayCountConvention.Thirty360European:
                    yearFraction = Days30E360(start, end) / 360m;
                    break;
                case DayCountConvention.ActualActualIsda:
                    yearFraction = ActualActualFraction(start, end);
                    break;
                default:
                    throw new ArgumentOutOfRangeException(nameof(convention), convention, "Unsupported day count.");
            }

            return Math.Round(principal * annualRate * yearFraction, 2, MidpointRounding.ToEven);
        }

        private static int Days30E360(DateTime start, DateTime end)
        {
            var d1 = Math.Min(start.Day, 30);
            var d2 = Math.Min(end.Day, 30);
            return 360 * (end.Year - start.Year) + 30 * (end.Month - start.Month) + (d2 - d1);
        }

        private static decimal ActualActualFraction(DateTime start, DateTime end)
        {
            var fraction = 0m;
            var cursor = start;
            while (cursor < end)
            {
                var nextYear = new DateTime(cursor.Year + 1, 1, 1);
                var segmentEnd = end < nextYear ? end : nextYear;
                var daysInYear = DateTime.IsLeapYear(cursor.Year) ? 366m : 365m;
                fraction += (segmentEnd - cursor).Days / daysInYear;
                cursor = segmentEnd;
            }
            return fraction;
        }
    }
}
