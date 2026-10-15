import { exec } from 'child_process';
import { promisify } from 'util';

const run = promisify(exec);

export async function stampLoanNumber(loanId: number, source: string, target: string): Promise<void> {
  const { stderr } = await run(
    `pdf-stamp --text "Loan ${loanId}" --position bottom-right --in ${source} --out ${target}`,
    { timeout: 30_000 },
  );
  if (stderr.trim().length > 0) {
    throw new Error(`pdf-stamp failed for loan ${loanId}`);
  }
}
