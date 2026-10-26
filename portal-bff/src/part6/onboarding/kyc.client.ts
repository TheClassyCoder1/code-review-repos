import { Injectable } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';
import { KycSubmission, KycSubmissionResponse } from './onboarding.dto';

/**
 * portal-bff -> loan-service KYC endpoints.
 *  - POST /api/v1/kyc           (loan-service KycController.submit)
 *  - GET  /api/v1/kyc/{userId}  (loan-service KycController.status)
 */
@Injectable()
export class KycClient {
  private readonly http: AxiosInstance;

  constructor() {
    this.http = axios.create({
      baseURL: process.env.LOAN_SERVICE_URL ?? 'http://localhost:8081',
      timeout: 5000,
    });
  }

  async submit(body: KycSubmission): Promise<KycSubmissionResponse> {
    const res = await this.http.post<KycSubmissionResponse>('/api/v1/kyc', body);
    return res.data;
  }

  async status(userId: number): Promise<KycSubmissionResponse | null> {
    const res = await this.http.get<KycSubmissionResponse | ''>(`/api/v1/kyc/${userId}`);
    // loan-service answers 200 with an empty body when nothing has been submitted yet
    return res.data === '' ? null : res.data;
  }
}
