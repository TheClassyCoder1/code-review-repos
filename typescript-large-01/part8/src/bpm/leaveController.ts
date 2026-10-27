import { CommonResult, HttpError, HttpRequest } from '../http/types';

export interface BpmOALeaveDO {
  id: number;
  userId: number;
  type: number;
  reason: string;
  startTime: Date;
  endTime: Date;
  day: number;
  status: number;
  processInstanceId: string;
  createTime: Date;
}

export interface BpmOALeaveRespVO {
  id: number;
  type: number;
  reason: string;
  startTime: string;
  endTime: string;
  day: number;
  status: number;
  processInstanceId: string;
  createTime: string;
}

export interface LeaveRepository {
  selectById(id: number): Promise<BpmOALeaveDO | null>;
}

export class BpmOALeaveService {
  constructor(private readonly leaves: LeaveRepository) {}

  getLeave(id: number): Promise<BpmOALeaveDO | null> {
    return this.leaves.selectById(id);
  }
}

function toRespVO(leave: BpmOALeaveDO | null): BpmOALeaveRespVO | null {
  if (!leave) {
    return null;
  }
  return {
    id: leave.id,
    type: leave.type,
    reason: leave.reason,
    startTime: leave.startTime.toISOString(),
    endTime: leave.endTime.toISOString(),
    day: leave.day,
    status: leave.status,
    processInstanceId: leave.processInstanceId,
    createTime: leave.createTime.toISOString(),
  };
}

function hasPermission(request: HttpRequest, permission: string): void {
  if (!request.user) {
    throw new HttpError(401, 'login required');
  }
  if (!request.user.permissions.includes(permission)) {
    throw new HttpError(403, 'forbidden');
  }
}

/** OA leave requests, mounted under /bpm/oa/leave. */
export class BpmOALeaveController {
  constructor(private readonly leaveService: BpmOALeaveService) {}

  /** GET /bpm/oa/leave/get?id= — fetch one leave request. */
  async getLeave(request: HttpRequest): Promise<CommonResult<BpmOALeaveRespVO | null>> {
    hasPermission(request, 'bpm:oa-leave:query');
    const id = Number(request.query['id']);
    const leave = await this.leaveService.getLeave(id);
    return CommonResult.success(toRespVO(leave));
  }
}
