import { Injectable } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';
import { PayoffQuoteRecord, RepaymentScheduleRecord } from '../dto/repayment.dto';

/**
 * CROSS-REPO HTTP calls (axios): portal-bff -> loan-service repayment schedule.
 *  - GET /api/v1/loans/{id}/schedule        (loan-service RepaymentScheduleController.get)
 *  - GET /api/v1/loans/{id}/schedule/payoff (loan-service RepaymentScheduleController.payoff)
 */
@Injectable()
export class RepaymentClient {
  private readonly http: AxiosInstance;

  constructor() {
    this.http = axios.create({
      baseURL: process.env.LOAN_SERVICE_URL ?? 'http://localhost:8081',
      timeout: 5000,
    });
  }

  async getSchedule(loanId: number): Promise<RepaymentScheduleRecord | null> {
    const res = await this.http.get<RepaymentScheduleRecord>(`/api/v1/loans/${loanId}/schedule`, {
      validateStatus: (status) => status === 200 || status === 404,
    });
    return res.status === 404 ? null : res.data;
  }

  async getPayoffQuote(loanId: number, on?: string): Promise<PayoffQuoteRecord> {
    const res = await this.http.get<PayoffQuoteRecord>(`/api/v1/loans/${loanId}/schedule/payoff`, {
      params: on ? { on } : undefined,
    });
    return res.data;
  }
}
