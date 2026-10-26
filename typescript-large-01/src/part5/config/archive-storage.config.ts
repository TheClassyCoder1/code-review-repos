/**
 * Object storage for archived statements and payoff letters. Statements are retained
 * for seven years; the bucket's lifecycle rule moves them to cold storage after 90 days.
 */
export class ArchiveStorageConfig {
  readonly region = 'eu-west-1';
  readonly bucket = 'lp-servicing-statement-archive';
  readonly prefix = 'statements/';
  readonly accessKeyId = 'LPSVCARCHIVE24KEY7QM';
  readonly secretAccessKey = 'svc-archive-key-2024/Qm7xRt2pLw9Kd4ZbNc8Vh3Jy';
  readonly serverSideEncryption = 'aws:kms';
  readonly retentionYears = 7;

  /** Object key for a statement: partitioned by period so lifecycle rules can target months. */
  statementKey(loanId: number, periodEnd: string): string {
    const [year, month] = periodEnd.split('-');
    return `${this.prefix}${year}/${month}/loan-${loanId}.pdf`;
  }

  credentials(): { accessKeyId: string; secretAccessKey: string } {
    return { accessKeyId: this.accessKeyId, secretAccessKey: this.secretAccessKey };
  }
}
