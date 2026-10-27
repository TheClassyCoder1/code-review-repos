import { createHash } from 'crypto';
import { HttpRequest } from '../http/types';

export const Sha256Tool = {
  sha256(input: string): string {
    return createHash('sha256').update(input, 'utf8').digest('hex');
  },
};

export class Response<T> {
  private constructor(readonly code: number, readonly msg: string | null, readonly data: T | null) {}

  static ofSuccess<T>(data: T | null = null): Response<T> {
    return new Response<T>(200, null, data);
  }

  static ofFail<T>(msg: string): Response<T> {
    return new Response<T>(500, msg, null);
  }
}

export interface JobUser {
  id: number;
  username: string;
  password: string;
  role: number;
}

export interface JobUserMapper {
  loadByUserName(username: string): Promise<JobUser | null>;
  update(user: JobUser): Promise<void>;
}

export interface LoginInfo {
  userId: string;
  userName: string;
}

export interface SsoHelper {
  /** Resolves the logged-in user from the request's SSO cookie. */
  loginCheckWithAttr(request: HttpRequest): Promise<Response<LoginInfo>>;
}

const messages: Record<string, string> = {
  system_please_input: 'Please input ',
  change_pwd_field_oldpwd: 'old password',
  system_length_limit: 'Length limit ',
  system_invalid: ' invalid',
};

function i18n(key: string): string {
  return messages[key] ?? key;
}

/** Account self-service for the job console, mounted under /user. */
export class PasswordController {
  constructor(private readonly jobUserMapper: JobUserMapper, private readonly ssoHelper: SsoHelper) {}

  /** POST /user/updatePwd */
  async updatePwd(request: HttpRequest, oldPassword: string | undefined, password: string | undefined): Promise<Response<string>> {
    // valid
    if (oldPassword == null || oldPassword.trim() === '') {
      return Response.ofFail(i18n('system_please_input') + i18n('change_pwd_field_oldpwd'));
    }
    if (password == null || password.trim() === '') {
      return Response.ofFail(i18n('system_please_input') + i18n('change_pwd_field_oldpwd'));
    }
    password = password.trim();
    if (!(password.length >= 4 && password.length <= 20)) {
      return Response.ofFail(i18n('system_length_limit') + '[4-20]');
    }

    // hash password
    const oldPasswordHash = Sha256Tool.sha256(oldPassword);
    const passwordHash = Sha256Tool.sha256(password);

    // valid old pwd
    const loginInfoResponse = await this.ssoHelper.loginCheckWithAttr(request);
    const existUser = await this.jobUserMapper.loadByUserName(loginInfoResponse.data!.userName);
    if (!existUser || oldPasswordHash !== existUser.password) {
      return Response.ofFail(i18n('change_pwd_field_oldpwd') + i18n('system_invalid'));
    }

    // write new
    existUser.password = passwordHash;
    await this.jobUserMapper.update(existUser);

    return Response.ofSuccess();
  }
}
