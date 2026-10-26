export interface DelinquentLoan {
  loanId: string;
  accountId: string;
  daysPastDue: number;
  overdueMinor: number;
}

export interface DelinquentPage {
  pageNumber: number;
  loans: DelinquentLoan[];
}

export interface DelinquencySource {
  pages(pageSize: number): AsyncIterable<DelinquentPage>;
}

export interface DelinquencySnapshot {
  loans: DelinquentLoan[];
  totalOverdueMinor: number;
}

/**
 * Materialises the delinquent book into a snapshot so dunning, charge-off and
 * the agent work queues all operate on the same consistent view.
 */
export class DelinquencyCollector {
  async collect(pages: AsyncIterable<DelinquentPage>): Promise<DelinquencySnapshot> {
    const loans: DelinquentLoan[] = [];
    for await (const page of pages) {
      for (const loan of page.loans) {
        loans.push(loan);
      }
    }
    const totalOverdueMinor = loans.reduce((sum, l) => sum + l.overdueMinor, 0);
    loans.sort((a, b) => b.daysPastDue - a.daysPastDue);
    return { loans, totalOverdueMinor };
  }
}
