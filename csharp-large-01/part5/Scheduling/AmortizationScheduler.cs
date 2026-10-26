using System;
using System.Collections.Generic;
using CsharpLarge01.Part5.Domain;

namespace CsharpLarge01.Part5.Scheduling
{
    /// <summary>
    /// Builds level-payment (French) amortisation schedules for newly disbursed loans.
    /// </summary>
    public sealed class AmortizationScheduler
    {
        private const int MaxTermMonths = 480;

        private readonly HolidayCalendar _calendar;

        public AmortizationScheduler(HolidayCalendar calendar)
        {
            _calendar = calendar ?? throw new ArgumentNullException(nameof(calendar));
        }

        public IReadOnlyList<Installment> BuildSchedule(decimal principal, decimal annualRate, int termMonths, DateTime firstDueDate)
        {
            if (principal <= 0m)
            {
                throw new ArgumentOutOfRangeException(nameof(principal), "Principal must be positive.");
            }
            if (annualRate < 0m || annualRate >= 1m)
            {
                throw new ArgumentOutOfRangeException(nameof(annualRate), "Annual rate is a fraction in [0, 1).");
            }
            if (termMonths <= 0 || termMonths > MaxTermMonths)
            {
                throw new ArgumentOutOfRangeException(nameof(termMonths));
            }

            var monthlyRate = annualRate / 12m;
            var payment = ComputeLevelPayment(principal, monthlyRate, termMonths);
            var schedule = new List<Installment>(termMonths);
            var balance = principal;

            for (var sequence = 1; sequence <= termMonths; sequence++)
            {
                var interest = Math.Round(balance * monthlyRate, 2, MidpointRounding.ToEven);
                var principalPart = sequence == termMonths ? balance : Math.Min(balance, payment - interest);
                // Offset from the first due date rather than the previous one so a
                // 31st-of-month start does not drift to the 28th after February.
                var dueDate = _calendar.NextBusinessDay(firstDueDate.AddMonths(sequence - 1));
                schedule.Add(new Installment(sequence, dueDate, principalPart, interest));
                balance -= principalPart;
            }

            return schedule;
        }

        public static decimal ComputeLevelPayment(decimal principal, decimal monthlyRate, int termMonths)
        {
            if (monthlyRate == 0m)
            {
                return Math.Round(principal / termMonths, 2, MidpointRounding.AwayFromZero);
            }

            var growth = 1m;
            for (var i = 0; i < termMonths; i++)
            {
                growth *= 1m + monthlyRate;
            }

            var payment = principal * monthlyRate * growth / (growth - 1m);
            return Math.Round(payment, 2, MidpointRounding.AwayFromZero);
        }
    }
}
