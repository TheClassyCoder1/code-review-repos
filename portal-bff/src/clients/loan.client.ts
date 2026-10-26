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
    // axios defaults to no timeout; a hung loan-service would otherwise hold portal requests open forever.
    this.http = axios.create({
      baseURL: process.env.LOAN_SERVICE_URL ?? 'http://localhost:8081',
      timeout: 5000,
    });
  }

  async createLoan(body: LoanApplicationRequest): Promise<RiskAssessment> {
    const res = await this.http.post<RiskAssessment>('/api/v1/loans', body);
    return res.data;
  }

  async getLoan(id: number): Promise<LoanRecord> {
    const res = await this.http.get<LoanRecord>(`/api/v1/loans/${id}`);
    return res.data;
  }
}
