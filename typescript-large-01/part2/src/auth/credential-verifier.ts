import { randomBytes, scrypt as scryptCb, timingSafeEqual } from 'crypto';
import { promisify } from 'util';

const scrypt = promisify(scryptCb) as (password: string, salt: Buffer, keylen: number) => Promise<Buffer>;

/** Borrower portal credentials, stored as salted scrypt hashes. */
export class CredentialVerifier {
  private readonly byLogin = new Map<string, { userId: string; salt: Buffer; hash: Buffer }>();

  async enrol(login: string, userId: string, password: string): Promise<void> {
    const salt = randomBytes(16);
    this.byLogin.set(login.toLowerCase(), { userId, salt, hash: await scrypt(password, salt, 64) });
  }

  /** Returns the borrower's internal id when the password matches, otherwise null. */
  async verify(login: string, password: string): Promise<string | null> {
    const stored = this.byLogin.get(login.toLowerCase());
    const candidate = await scrypt(password, stored?.salt ?? randomBytes(16), 64);
    return stored && timingSafeEqual(candidate, stored.hash) ? stored.userId : null;
  }
}
