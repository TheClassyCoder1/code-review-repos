import { Injectable } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';
import { LoanStatement } from './statement.dto';

/** portal-bff -> loan-service GET /api/v1/loans/{id}/statement (loan-service StatementController). */
@Injectable()
export class StatementClient {
  private readonly http: AxiosInstance;

  constructor() {
    this.http = axios.create({
      baseURL: process.env.LOAN_SERVICE_URL ?? 'http://localhost:8081',
      timeout: 5000,
    });
  }

  async latest(loanId: number): Promise<LoanStatement | null> {
    const res = await this.http.get<LoanStatement | ''>(`/api/v1/loans/${loanId}/statement`);
    return res.data === '' ? null : res.data;
  }
}
