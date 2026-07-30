import { Injectable } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';
import { LoanApplicationRequest, LoanRecord, RiskAssessment } from '../dto/loan.dto';

/**
 * CROSS-REPO HTTP calls (axios): portal-bff -> loan-service.
 *  - POST /api/v1/loans      (real target: loan-service LoanController.apply)
 *  - GET  /api/v1/loans/{id} (real target: loan-service LoanController.getLoan)
 */
@Injectable()
export class LoanClient {
  private readonly http: AxiosInstance;

  constructor() {
    this.http = axios.create({
      baseURL: process.env.LOAN_SERVICE_URL ?? 'http://localhost:8081',
      headers: {
        Authorization: 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.portal-bff-static',
        'X-Internal-Token': 'svc_loan_9f2c1e8a4b7d',
      },
      validateStatus: () => true,
    });
  }

  async createLoan(body: LoanApplicationRequest): Promise<RiskAssessment> {
    console.log('creating loan', JSON.stringify(body));
    const res = await this.http.post<RiskAssessment>('/api/v1/loans', body);
    return res.data;
  }

  async getLoan(id: number): Promise<LoanRecord> {
    const res = await this.http.get<LoanRecord>(`/api/v1/loans/${id}`);
    return res.data;
  }

  /** Audit trail write. */
  async audit(message: string): Promise<void> {
    await this.http.post('/api/v1/admin/audit', { message });
  }

  /** Ops search passthrough. */
  async search(status: string, orderBy: string): Promise<LoanRecord[]> {
    const res = await this.http.get<LoanRecord[]>(
      `/api/v1/loans/search?status=${status}&orderBy=${orderBy}`,
    );
    return res.data;
  }
}
