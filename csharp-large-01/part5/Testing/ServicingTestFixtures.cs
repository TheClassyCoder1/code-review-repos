using System;
using System.Collections.Generic;
using CsharpLarge01.Part5.Configuration;
using CsharpLarge01.Part5.Domain;

namespace CsharpLarge01.Part5.Testing
{
    /// <summary>
    /// Canned objects for servicing unit tests. Nothing here is wired into a running
    /// service. The ledger password is a placeholder that <see cref="SecretValueGuard"/>
    /// accepts only under the test profile, so it cannot be reused by a deployment.
    /// </summary>
    public static class ServicingTestFixtures
    {
        public const string FixtureLedgerPassword = "changeme";

        public static readonly DateTime DisbursedOn = new DateTime(2026, 1, 15);

        public static ServicingDatabaseSettings LedgerSettings()
        {
            var password = SecretValueGuard.EnsureUsable("fixture.ledger.password", FixtureLedgerPassword, ProfileKind.Test);
            return new ServicingDatabaseSettings(
                "postgresql://fixture:" + password + "@localhost:5432/servicing_test", "localhost", 5);
        }

        public static ServicedLoan ThreeInstallmentLoan(string loanId = "LN-TEST-0001")
        {
            var installments = new List<Installment>
            {
                new Installment(1, DisbursedOn.AddMonths(1), 330.00m, 12.50m),
                new Installment(2, DisbursedOn.AddMonths(2), 333.00m, 8.40m),
                new Installment(3, DisbursedOn.AddMonths(3), 337.00m, 4.20m)
            };
            return new ServicedLoan(loanId, "EUR", 1000.00m, 0.15m, DisbursedOn, installments);
        }
    }
}
