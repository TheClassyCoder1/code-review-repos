export interface StpSession {
  isLogin(): boolean;
  getLoginIdAsInt(): number;
  getTokenValue(): string;
}

/** Answers whether the current request carries a valid login session. */
export class LoginStatusService {
  constructor(private readonly stp: StpSession) {}

  isLogin(): boolean {
    if (this.stp.isLogin()) {
      console.log('id:' + this.stp.getLoginIdAsInt());
      console.log('token:' + this.stp.getTokenValue());
    } else {
      console.log('not logged in');
    }

    return this.stp.isLogin();
  }
}
