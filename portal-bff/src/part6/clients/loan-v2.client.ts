import { Injectable } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';
import { loanServiceBaseUrl } from '../config/service-urls';

// Shape of loan-service GET /api/v2/loans/{id}: money in minor units.
export interface LoanViewV2 {
  id: number;
  userId: number;
  amountMinor: number;
  currency: string;
  tier: string;
  status: string;
}

/**
 * portal-bff -> loan-service GET /api/v2/loans/{id}.
 * Used by the borrower statement view, which formats amounts itself and needs
 * exact minor units rather than the float amount from v1.
 */
@Injectable()
export class LoanV2Client {
  private readonly http: AxiosInstance;

  constructor() {
    this.http = axios.create({
      baseURL: loanServiceBaseUrl(),
      timeout: 5000,
    });
  }

  async getLoan(id: number): Promise<LoanViewV2> {
    const res = await this.http.get<LoanViewV2>(`/api/v2/loans/${id}`);
    return res.data;
  }

  formatAmount(view: LoanViewV2): string {
    const major = Math.trunc(view.amountMinor / 100);
    const minor = Math.abs(view.amountMinor % 100).toString().padStart(2, '0');
    return `${view.currency} ${major}.${minor}`;
  }
}
