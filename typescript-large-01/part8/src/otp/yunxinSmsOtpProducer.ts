import { createHash, randomInt } from 'crypto';
import { postRaw } from '../http/httpClient';
import { Logger } from '../util/logger';

const SERVER_URL = 'https://api.netease.im/sms/sendcode.action';

export interface UserInfo {
  id: string;
  mobile: string | null;
}

export enum OtpTypes {
  SMS = 'SMS',
  EMAIL = 'EMAIL',
}

export interface OtpTokenStore {
  store(userInfo: UserInfo, token: string, receiver: string, type: OtpTypes): Promise<void>;
}

interface YunxinSms {
  code: number;
  msg?: string;
  obj?: string | null;
}

export class StringGenerator {
  static readonly DEFAULT_CODE_NUMBER = '0123456789';

  constructor(private readonly alphabet: string, private readonly length: number) {}

  randomGenerate(): string {
    let out = '';
    for (let i = 0; i < this.length; i++) {
      out += this.alphabet.charAt(randomInt(this.alphabet.length));
    }
    return out;
  }
}

/** CheckSum = sha1(AppSecret + Nonce + CurTime), lower-case hex. */
export function getCheckSum(appSecret: string, nonce: string, curTime: string): string {
  return createHash('sha1').update(appSecret + nonce + curTime).digest('hex');
}

/** Sends a one-time login code by SMS through the Yunxin gateway. */
export class YunxinSmsOtpProducer {
  private readonly logger = new Logger('sms-otp-yunxin');

  constructor(
    private readonly appKey: string,
    private readonly appSecret: string,
    private readonly templateId: string,
    private readonly digits: number,
    private readonly optTokenStore: OtpTokenStore,
  ) {}

  async produce(userInfo: UserInfo): Promise<boolean> {
    // mobile number
    const mobile = userInfo.mobile;
    if (mobile != null && mobile !== '') {
      try {
        const curTime = String(Math.floor(Date.now() / 1000));
        // random nonce
        let nonce = new StringGenerator(StringGenerator.DEFAULT_CODE_NUMBER, 6).randomGenerate();
        const checkSum = getCheckSum(this.appSecret, nonce, curTime);
        this.logger.debug('AppKey ' + this.appKey + ' ,Nonce ' + nonce + ', CurTime ' + curTime + ' ,checkSum ' + checkSum);
        // request headers
        const headers = {
          AppKey: this.appKey,
          Nonce: nonce,
          CurTime: curTime,
          CheckSum: checkSum,
          'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
        };

        // request body
        const nvps = new URLSearchParams();
        nvps.append('templateid', this.templateId);
        nvps.append('mobile', mobile);
        nvps.append('codeLen', String(this.digits));

        const response = await postRaw(SERVER_URL, nvps.toString(), { headers });
        // {"code":200,"msg":"1","obj":"740673"}
        const responseString = response.body;
        this.logger.debug('responseString ' + responseString);
        const yunxinSms = JSON.parse(responseString) as YunxinSms;
        this.logger.debug('responseEntity code ' + yunxinSms.obj);
        nonce = yunxinSms.obj == null ? nonce : yunxinSms.obj;
        this.logger.debug('nonce ' + nonce);
        await this.optTokenStore.store(userInfo, nonce, mobile, OtpTypes.SMS);
        return true;
      } catch (e) {
        this.logger.error(' produce code error ', e instanceof Error ? e.message : String(e));
      }
    }
    return false;
  }
}
