export class AjaxJson {
  static readonly CODE_SUCCESS = 200;
  static readonly CODE_ERROR = 500;
  static readonly CODE_NOT_JUR = 403;
  static readonly CODE_NOT_LOGIN = 401;

  constructor(public code: number, public msg: string, public data: unknown = null) {}

  setMsg(msg: string): this {
    this.msg = msg;
    return this;
  }

  static getNotLogin(): AjaxJson {
    return new AjaxJson(AjaxJson.CODE_NOT_LOGIN, 'not logged in');
  }

  static getNotJur(msg: string): AjaxJson {
    return new AjaxJson(AjaxJson.CODE_NOT_JUR, msg);
  }

  static getError(msg: string): AjaxJson {
    return new AjaxJson(AjaxJson.CODE_ERROR, msg);
  }
}

export class NotLoginException extends Error {}

export class NotRoleException extends Error {
  constructor(readonly role: string) {
    super(`missing role ${role}`);
  }
}

export class NotPermissionException extends Error {
  constructor(readonly permission: string) {
    super(`missing permission ${permission}`);
  }
}

export class DisableServiceException extends Error {
  constructor(readonly service: string, readonly level: number, readonly disableTime: number) {
    super(`service ${service} disabled`);
  }
}

/** Maps any error thrown by a controller into the AjaxJson envelope the console expects. */
export function handlerException(e: Error): AjaxJson {
  // print the stack for debugging
  console.log('global exception---------------');
  console.error(e);

  // different exceptions map to different status codes
  let aj: AjaxJson;
  if (e instanceof NotLoginException) {
    aj = AjaxJson.getNotLogin().setMsg(e.message);
  } else if (e instanceof NotRoleException) {
    aj = AjaxJson.getNotJur('missing role: ' + e.role);
  } else if (e instanceof NotPermissionException) {
    aj = AjaxJson.getNotJur('missing permission: ' + e.permission);
  } else if (e instanceof DisableServiceException) {
    aj = AjaxJson.getNotJur('service ' + e.service + ' is disabled for this account (level=' + e.level + '): unlocks in ' + e.disableTime + 's');
  } else {
    // generic error: 500 + error message
    aj = AjaxJson.getError(e.message);
  }

  // return to the front end
  return aj;
}
